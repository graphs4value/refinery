/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.ide.contentassist;

import com.google.inject.Inject;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.xtext.CrossReference;
import org.eclipse.xtext.GrammarUtil;
import org.eclipse.xtext.ide.editor.contentassist.ContentAssistContext;
import org.eclipse.xtext.ide.editor.contentassist.IdeCrossrefProposalProvider;
import org.eclipse.xtext.naming.QualifiedName;
import org.eclipse.xtext.nodemodel.util.NodeModelUtils;
import org.eclipse.xtext.resource.IEObjectDescription;
import org.eclipse.xtext.scoping.IScope;
import tools.refinery.language.model.problem.Problem;
import tools.refinery.language.resource.ProblemResourceDescriptionStrategy;
import tools.refinery.language.resource.ReferenceCounter;
import tools.refinery.language.utils.ProblemUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

public class ProblemCrossrefProposalProvider extends IdeCrossrefProposalProvider {
	@Inject
	private ReferenceCounter referenceCounter;

	@Override
	protected Iterable<IEObjectDescription> queryScope(IScope scope, CrossReference crossReference,
													   ContentAssistContext context) {
		var eObjectDescriptionsByName = new HashMap<QualifiedName, List<IEObjectDescription>>();
		for (var candidate : super.queryScope(scope, crossReference, context)) {
			if (isExistingObject(candidate, crossReference, context)) {
				// {@code getQualifiedName()} will refer to the full name for objects that are loaded from the global
				// scope, but {@code getName()} returns the qualified name that we set in
				// {@code ProblemResourceDescriptionStrategy}.
				var qualifiedName = candidate.getName();
				var candidateList = eObjectDescriptionsByName.computeIfAbsent(qualifiedName,
						ignored -> new ArrayList<>());
				candidateList.add(candidate);
			}
		}
		var eObjectDescriptions = new ArrayList<IEObjectDescription>();
		for (var candidates : eObjectDescriptionsByName.values()) {
			if (candidates.size() == 1) {
				var candidate = candidates.get(0);
				if (shouldBeVisible(candidate)) {
					eObjectDescriptions.add(candidate);
				}
			}
		}
		return eObjectDescriptions;
	}

	protected boolean isExistingObject(IEObjectDescription candidate, CrossReference crossRef,
									   ContentAssistContext context) {
		var rootModel = context.getRootModel();
		var eObjectOrProxy = candidate.getEObjectOrProxy();
		if (!Objects.equals(rootModel.eResource(), eObjectOrProxy.eResource())) {
			return true;
		}
		var currentValue = getCurrentValue(crossRef, context);
		if (currentValue == null) {
			return true;
		}
		var eObject = EcoreUtil.resolve(eObjectOrProxy, rootModel);
		if (!Objects.equals(currentValue, eObject)) {
			return true;
		}
		if (!ProblemUtil.isImplicit(eObject)) {
			return true;
		}
		if (rootModel instanceof Problem problem) {
			return referenceCounter.countReferences(problem, eObject) >= 2;
		}
		return true;
	}

	protected boolean shouldBeVisible(IEObjectDescription candidate) {
		var errorPredicate = candidate.getUserData(ProblemResourceDescriptionStrategy.ERROR_PREDICATE);
		return !ProblemResourceDescriptionStrategy.ERROR_PREDICATE_TRUE.equals(errorPredicate);
	}

	protected EObject getCurrentValue(CrossReference crossRef, ContentAssistContext context) {
		var value = getCurrentValue(crossRef, context.getCurrentModel());
		if (value != null) {
			return value;
		}
		var currentNodeSemanticObject = NodeModelUtils.findActualSemanticObjectFor(context.getCurrentNode());
		return getCurrentValue(crossRef, currentNodeSemanticObject);
	}

	protected EObject getCurrentValue(CrossReference crossRef, EObject context) {
		if (context == null) {
			return null;
		}
		var eReference = GrammarUtil.getReference(crossRef, context.eClass());
		if (eReference == null || eReference.isMany()) {
			return null;
		}
		return (EObject) context.eGet(eReference);
	}
}
