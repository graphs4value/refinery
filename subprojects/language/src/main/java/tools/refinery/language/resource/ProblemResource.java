/*******************************************************************************
 * Copyright (c) 2008, 2023 itemis AG (http://www.itemis.eu) and others.
 * Copyright (c) 2023 The Refinery Authors <https://refinery.tools/>
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.language.resource;

import com.google.inject.Inject;
import org.apache.log4j.Logger;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.xtext.EcoreUtil2;
import org.eclipse.xtext.diagnostics.DiagnosticMessage;
import org.eclipse.xtext.diagnostics.Severity;
import org.eclipse.xtext.linking.ILinkingDiagnosticMessageProvider;
import org.eclipse.xtext.linking.impl.IllegalNodeException;
import org.eclipse.xtext.linking.impl.XtextLinkingDiagnostic;
import org.eclipse.xtext.linking.lazy.LazyLinkingResource;
import org.eclipse.xtext.nodemodel.INode;
import org.eclipse.xtext.parser.IParseResult;
import org.eclipse.xtext.resource.DerivedStateAwareResource;
import org.eclipse.xtext.util.Triple;
import org.jetbrains.annotations.Nullable;
import tools.refinery.language.model.problem.Problem;
import tools.refinery.language.utils.ProblemUtil;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class ProblemResource extends DerivedStateAwareResource {
	private static final Logger log = Logger.getLogger(ProblemResource.class);

	@Inject
	private ILinkingDiagnosticMessageProvider.Extended linkingDiagnosticMessageProvider;

	/**
	 * Our own version of this field, because the original is not accessible.
	 */
	private int cyclicLinkingDetectionCounter = 0;

	@Override
	protected void updateInternalState(IParseResult oldParseResult, IParseResult newParseResult) {
		if (isNewRootElement(oldParseResult, newParseResult) &&
				newParseResult.getRootASTElement() instanceof Problem newRootProblem &&
				!newRootProblem.isExplicitKind()) {
			// Post-process the parsed model to set its URI-dependent module kind.
			// We can't set the default module kind in {@link tools.refinery.language.serializer
			// .ProblemTransientValueService}, because the {@link Problem} does not get added into the EMF resource
			// before parsing is fully completed.
			var defaultModuleKind = ProblemUtil.getDefaultModuleKind(getURI());
			newRootProblem.setKind(defaultModuleKind);
		}
		super.updateInternalState(oldParseResult, newParseResult);
	}

	private boolean isNewRootElement(IParseResult oldParseResult, IParseResult newParseResult) {
		if (oldParseResult == null) {
			return true;
		}
		var oldRootAstElement = oldParseResult.getRootASTElement();
		var newRootAstElement = newParseResult.getRootASTElement();
		return oldRootAstElement != newRootAstElement;
	}

	/**
	 * Tries to resolve a reference and emits a diagnostic if the reference is unresolvable or ambiguous.
	 * <p>
	 * This method was copied from {@link LazyLinkingResource#getEObject(String, Triple)}, but we modified it to also
	 * handle ambiguous references.
	 *
	 * @param uriFragment The URI fragment to resolve.
	 * @param triple      The linking triple.
	 * @return The resolved {@link EObject}.
	 * @throws AssertionError If the URI fragment is unresolvable.
	 */
	@Override
	protected EObject getEObject(String uriFragment, Triple<EObject, EReference, INode> triple) throws AssertionError {
		cyclicLinkingDetectionCounter++;
		if (cyclicLinkingDetectionCounter > cyclicLinkingDectectionCounterLimit && !resolving.add(triple)) {
			return handleCyclicResolution(triple);
		}
		try {
			Set<String> unresolvableProxies = getUnresolvableURIFragments();
			if (unresolvableProxies.contains(uriFragment)) {
				return null;
			}
			var result = doGetEObject(triple);
			if (result == null) {
				if (isUnresolveableProxyCacheable(triple)) {
					unresolvableProxies.add(uriFragment);
				}
			} else {
				// remove previously added error markers, since everything should be fine now
				unresolvableProxies.remove(uriFragment);
			}
			return result;
		} catch (IllegalNodeException e) {
			createAndAddDiagnostic(triple, e);
			return null;
		} finally {
			if (cyclicLinkingDetectionCounter > cyclicLinkingDectectionCounterLimit) {
				resolving.remove(triple);
			}
			cyclicLinkingDetectionCounter--;
		}
	}

	@Nullable
	private EObject doGetEObject(Triple<EObject, EReference, INode> triple) {
		EReference reference = triple.getSecond();
		try {
			List<EObject> linkedObjects = getLinkingService().getLinkedObjects(triple.getFirst(), reference,
					triple.getThird());
			if (linkedObjects.isEmpty()) {
				createAndAddDiagnostic(triple);
				return null;
			}
			if (linkedObjects.size() > 1) {
				createAndAddAmbiguousReferenceDiagnostic(triple);
				return null;
			}
			EObject result = linkedObjects.get(0);
			if (!EcoreUtil2.isAssignableFrom(reference.getEReferenceType(), result.eClass())) {
				log.error("An element of type %s is not assignable to the reference %s.%s".formatted(
						result.getClass().getName(), reference.getEContainingClass().getName(), reference.getName()));
				createAndAddDiagnostic(triple);
				return null;
			}
			removeDiagnostic(triple);
			return result;
		} catch (CyclicLinkingError e) {
			if (e.triple.equals(triple)) {
				log.error(e.getMessage(), e);
				createAndAddDiagnostic(triple);
				return null;
			} else {
				throw e;
			}
		}
	}

	@Override
	protected EObject handleCyclicResolution(Triple<EObject, EReference, INode> triple) throws AssertionError {
		// Throw our own version of {@link LazyLinkingResource.CyclicLinkingException}.
		throw new CyclicLinkingError("Cyclic resolution of lazy links : %s in resource '%s'.".formatted(
				getReferences(triple, resolving), getURI()), triple);
	}

	@Override
	protected void createAndAddDiagnostic(Triple<EObject, EReference, INode> triple) {
		if (isValidationDisabled()) {
			return;
		}
		DiagnosticMessage message = createDiagnosticMessage(triple);
		addOrReplaceDiagnostic(triple, message);
	}

	@Override
	protected void createAndAddDiagnostic(Triple<EObject, EReference, INode> triple, IllegalNodeException ex) {
		if (isValidationDisabled()) {
			return;
		}
		ILinkingDiagnosticMessageProvider.ILinkingDiagnosticContext context = createDiagnosticMessageContext(triple);
		DiagnosticMessage message = linkingDiagnosticMessageProvider.getIllegalNodeMessage(context, ex);
		addOrReplaceDiagnostic(triple, message);
	}

	protected void createAndAddAmbiguousReferenceDiagnostic(Triple<EObject, EReference, INode> triple) {
		if (isValidationDisabled()) {
			return;
		}
		var context = createDiagnosticMessageContext(triple);
		var typeName = context.getReference().getEReferenceType().getName();
		String linkText = "";
		try {
			linkText = context.getLinkText();
		} catch (IllegalNodeException e) {
			linkText = e.getNode().getText();
		}
		var messageString = "Ambiguous reference to %s '%s'.".formatted(typeName, linkText);
		var message = new DiagnosticMessage(messageString, Severity.ERROR,
				org.eclipse.xtext.diagnostics.Diagnostic.LINKING_DIAGNOSTIC);
		addOrReplaceDiagnostic(triple, message);
	}

	/**
	 * Adds a diagnostic message while maintaining the invariant that at most one
	 * {@link ProblemResourceLinkingDiagnostic} is added to the {@link #getErrors()} list.
	 *
	 * @param triple  The triple to add the diagnostic for.
	 * @param message The diagnostic message. Must have {@link Severity#ERROR}.
	 */
	protected void addOrReplaceDiagnostic(Triple<EObject, EReference, INode> triple, DiagnosticMessage message) {
		if (message == null) {
			return;
		}
		if (message.getSeverity() != Severity.ERROR) {
			throw new IllegalArgumentException("Only linking diagnostics of ERROR severity are supported");
		}
		var list = getDiagnosticList(message);
		var iterator = list.iterator();
		while (iterator.hasNext()) {
			var diagnostic = iterator.next();
			if (diagnostic instanceof ProblemResourceLinkingDiagnostic linkingDiagnostic &&
					linkingDiagnostic.matchesNode(triple.getThird())) {
				if (linkingDiagnostic.matchesMessage(message)) {
					return;
				}
				iterator.remove();
				break;
			}
		}
		var diagnostic = createDiagnostic(triple, message);
		list.add(diagnostic);
	}

	/**
	 * Removes the {@link ProblemResourceLinkingDiagnostic} corresponding to the given node, if prevesent, from the
	 * {@link #getErrors()} list.
	 *
	 * @param triple The triple to add the diagnostic for.
	 */
	@Override
	protected void removeDiagnostic(Triple<EObject, EReference, INode> triple) {
		if (getErrors().isEmpty()) {
			return;
		}
		var list = getErrors();
		if (list.isEmpty()) {
			return;
		}
		var iterator = list.iterator();
		while (iterator.hasNext()) {
			var diagnostic = iterator.next();
			if (diagnostic instanceof ProblemResourceLinkingDiagnostic linkingDiagnostic &&
					linkingDiagnostic.matchesNode(triple.getThird())) {
				iterator.remove();
				return;
			}
		}
	}

	@Override
	protected Diagnostic createDiagnostic(Triple<EObject, EReference, INode> triple, DiagnosticMessage message) {
		return new ProblemResourceLinkingDiagnostic(triple.getThird(), message.getMessage(),
				message.getIssueCode(), message.getIssueData());
	}

	/**
	 * Our own version of {@link LazyLinkingResource.CyclicLinkingException}, because the {@code tripe} field in the
	 * original one is not accessible.
	 * <p>
	 * Renamed from {@code CyclicLinkingException} to satisfy naming conventions enforced by Sonar.
	 */
	public static final class CyclicLinkingError extends AssertionError {
		private final transient Triple<EObject, EReference, INode> triple;

		private CyclicLinkingError(Object detailMessage, Triple<EObject, EReference, INode> triple) {
			super(detailMessage);
			this.triple = triple;
		}
	}

	/**
	 * Marks all diagnostics inserted by {@link ProblemResource} with a common superclass so that they can
	 * later be removed.
	 * <p>
	 * We have to inherit from {@link XtextLinkingDiagnostic} to access the protected function {@link #getNode()}.
	 */
	protected static class ProblemResourceLinkingDiagnostic extends XtextLinkingDiagnostic {
		public ProblemResourceLinkingDiagnostic(INode node, String message, String code, String... data) {
			super(node, message, code, data);
		}

		public boolean matchesNode(INode node) {
			return Objects.equals(getNode(), node);
		}

		public boolean matchesMessage(DiagnosticMessage message) {
			return Objects.equals(getMessage(), message.getMessage()) &&
					Objects.equals(getCode(), message.getIssueCode()) &&
					Arrays.equals(getData(), message.getIssueData());
		}
	}
}
