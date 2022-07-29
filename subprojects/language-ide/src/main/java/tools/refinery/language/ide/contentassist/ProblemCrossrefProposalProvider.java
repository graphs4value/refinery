package tools.refinery.language.ide.contentassist;

import java.util.Objects;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.xtext.CrossReference;
import org.eclipse.xtext.GrammarUtil;
import org.eclipse.xtext.ide.editor.contentassist.ContentAssistContext;
import org.eclipse.xtext.ide.editor.contentassist.ContentAssistEntry;
import org.eclipse.xtext.ide.editor.contentassist.IdeCrossrefProposalProvider;
import org.eclipse.xtext.nodemodel.util.NodeModelUtils;
import org.eclipse.xtext.resource.IEObjectDescription;

import com.google.inject.Inject;

import tools.refinery.language.ProblemUtil;
import tools.refinery.language.model.problem.Problem;
import tools.refinery.language.resource.ReferenceCounter;

public class ProblemCrossrefProposalProvider extends IdeCrossrefProposalProvider {
	@Inject
	private ReferenceCounter referenceCounter;

	@Override
	protected ContentAssistEntry createProposal(IEObjectDescription candidate, CrossReference crossRef,
			ContentAssistContext context) {
		if (!shouldCreateProposal(candidate, crossRef, context)) {
			return null;
		}
		return super.createProposal(candidate, crossRef, context);
	}

	protected boolean shouldCreateProposal(IEObjectDescription candidate, CrossReference crossRef,
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
