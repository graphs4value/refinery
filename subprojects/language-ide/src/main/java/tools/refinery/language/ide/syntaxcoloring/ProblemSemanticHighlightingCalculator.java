/*
 * SPDX-FileCopyrightText: 2021-2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.ide.syntaxcoloring;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.xtext.ide.editor.syntaxcoloring.DefaultSemanticHighlightingCalculator;
import org.eclipse.xtext.ide.editor.syntaxcoloring.IHighlightedPositionAcceptor;
import org.eclipse.xtext.nodemodel.INode;
import org.eclipse.xtext.nodemodel.util.NodeModelUtils;
import org.eclipse.xtext.service.OperationCanceledManager;
import org.eclipse.xtext.util.CancelIndicator;
import org.jetbrains.annotations.NotNull;
import tools.refinery.language.model.problem.*;
import tools.refinery.language.utils.ProblemDesugarer;
import tools.refinery.language.utils.ProblemUtil;

import java.util.List;

public class ProblemSemanticHighlightingCalculator extends DefaultSemanticHighlightingCalculator {
	private static final String BUILTIN_CLASS = "builtin";
	private static final String ABSTRACT_CLASS = "abstract";
	private static final String CONTAINMENT_CLASS = "containment";
	private static final String ERROR_CLASS = "error";
	private static final String NODE_CLASS = "node";
	private static final String ATOM_NODE_CLASS = "atom";
	private static final String NEW_NODE_CLASS = "new";

	@Inject
	private OperationCanceledManager operationCanceledManager;

	@Inject
	private ProblemDesugarer desugarer;

	@Inject
	private TypeHashProvider typeHashProvider;

	@Override
	protected boolean highlightElement(EObject object, IHighlightedPositionAcceptor acceptor,
									   CancelIndicator cancelIndicator) {
		highlightName(object, acceptor);
		highlightCrossReferences(object, acceptor, cancelIndicator);
		return false;
	}

	protected void highlightName(EObject object, IHighlightedPositionAcceptor acceptor) {
		if (!(object instanceof NamedElement)) {
			return;
		}
		String[] highlightClass = getHighlightClass(object, null);
		if (highlightClass.length > 0) {
			highlightFeature(acceptor, object, ProblemPackage.Literals.NAMED_ELEMENT__NAME, highlightClass);
		}
	}

	protected void highlightCrossReferences(EObject object, IHighlightedPositionAcceptor acceptor,
											CancelIndicator cancelIndicator) {
		for (EReference reference : object.eClass().getEAllReferences()) {
			if (reference.isContainment()) {
				continue;
			}
			operationCanceledManager.checkCanceled(cancelIndicator);
			if (reference.isMany()) {
				highlightManyValues(object, reference, acceptor);
			} else {
				highlightSingleValue(object, reference, acceptor);
			}
		}
	}

	protected void highlightSingleValue(EObject object, EReference reference, IHighlightedPositionAcceptor acceptor) {
		EObject valueObj = (EObject) object.eGet(reference);
		String[] highlightClass = getHighlightClass(valueObj, reference);
		if (highlightClass.length > 0) {
			highlightFeature(acceptor, object, reference, highlightClass);
		}
	}

	protected void highlightManyValues(EObject object, EReference reference, IHighlightedPositionAcceptor acceptor) {
		@SuppressWarnings("unchecked")
		EList<? extends EObject> values = (EList<? extends EObject>) object.eGet(reference);
		List<INode> nodes = NodeModelUtils.findNodesForFeature(object, reference);
		int size = Math.min(values.size(), nodes.size());
		for (var i = 0; i < size; i++) {
			EObject valueInList = values.get(i);
			INode node = nodes.get(i);
			String[] highlightClass = getHighlightClass(valueInList, reference);
			if (highlightClass.length > 0) {
				highlightNode(acceptor, node, highlightClass);
			}
		}
	}

	protected String[] getHighlightClass(EObject eObject, EReference reference) {
		boolean isError = ProblemUtil.isError(eObject);
		if (ProblemUtil.isBuiltIn(eObject) && !(eObject instanceof Problem)) {
			var className = isError ? ERROR_CLASS : BUILTIN_CLASS;
			return new String[]{className};
		}
		return getUserDefinedElementHighlightClass(eObject, reference, isError);
	}

	@NotNull
	private String[] getUserDefinedElementHighlightClass(EObject eObject, EReference reference, boolean isError) {
		ImmutableList.Builder<String> classesBuilder = ImmutableList.builder();
		if (eObject instanceof ClassDeclaration classDeclaration && classDeclaration.isAbstract()) {
			classesBuilder.add(ABSTRACT_CLASS);
		}
		if (eObject instanceof ReferenceDeclaration referenceDeclaration
				&& desugarer.isContainmentReference(referenceDeclaration)) {
			classesBuilder.add(CONTAINMENT_CLASS);
		}
		if (isError && reference != null) {
			// References to error patterns should be highlighted as errors, but error pattern definitions shouldn't.
			classesBuilder.add(ERROR_CLASS);
		}
		if (eObject instanceof Node node) {
			highlightNode(node, reference, classesBuilder);
		}
		if (eObject instanceof Relation relation) {
			var typeHash = typeHashProvider.getTypeHash(relation);
			if (typeHash != null) {
				classesBuilder.add("typeHash-" + typeHash);
			}
		}
		List<String> classes = classesBuilder.build();
		return classes.toArray(new String[0]);
	}

	private static void highlightNode(Node node, EReference reference, ImmutableList.Builder<String> classesBuilder) {
		if (reference == ProblemPackage.Literals.VARIABLE_OR_NODE_EXPR__VARIABLE_OR_NODE) {
			classesBuilder.add(NODE_CLASS);
		}
		if (ProblemUtil.isAtomNode(node)) {
			classesBuilder.add(ATOM_NODE_CLASS);
		}
		if (ProblemUtil.isMultiNode(node)) {
			classesBuilder.add(NEW_NODE_CLASS);
		}
	}
}
