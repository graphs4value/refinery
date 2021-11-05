package tools.refinery.language.ide.syntaxcoloring;

import java.util.List;

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.xtext.ide.editor.syntaxcoloring.DefaultSemanticHighlightingCalculator;
import org.eclipse.xtext.ide.editor.syntaxcoloring.IHighlightedPositionAcceptor;
import org.eclipse.xtext.nodemodel.INode;
import org.eclipse.xtext.nodemodel.util.NodeModelUtils;
import org.eclipse.xtext.service.OperationCanceledManager;
import org.eclipse.xtext.util.CancelIndicator;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;

import tools.refinery.language.model.ProblemUtil;
import tools.refinery.language.model.problem.ClassDeclaration;
import tools.refinery.language.model.problem.NamedElement;
import tools.refinery.language.model.problem.Node;
import tools.refinery.language.model.problem.PredicateDefinition;
import tools.refinery.language.model.problem.ProblemPackage;
import tools.refinery.language.model.problem.ReferenceDeclaration;

public class ProblemSemanticHighlightingCalculator extends DefaultSemanticHighlightingCalculator {
	private static final String BUILTIN_CLASS = "builtin";
	private static final String ABSTRACT_CLASS = "abstract";
	private static final String CONTAINMENT_CLASS = "containment";
	private static final String ERROR_CLASS = "error";
	private static final String NODE_CLASS = "node";
	private static final String INDIVIDUAL_NODE_CLASS = "individual";
	private static final String NEW_NODE_CLASS = "new";

	@Inject
	private OperationCanceledManager operationCanceledManager;

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
		if (ProblemUtil.isBuiltIn(eObject)) {
			return new String[] { BUILTIN_CLASS };
		}
		ImmutableList.Builder<String> classesBuilder = ImmutableList.builder();
		if (eObject instanceof ClassDeclaration classDeclaration && classDeclaration.isAbstract()) {
			classesBuilder.add(ABSTRACT_CLASS);
		}
		if (eObject instanceof ReferenceDeclaration referenceDeclaration && referenceDeclaration.isContainment()) {
			classesBuilder.add(CONTAINMENT_CLASS);
		}
		if (eObject instanceof PredicateDefinition predicateDefinition && predicateDefinition.isError()) {
			classesBuilder.add(ERROR_CLASS);
		}
		if (eObject instanceof Node node) {
			if (reference == ProblemPackage.Literals.VARIABLE_OR_NODE_ARGUMENT__VARIABLE_OR_NODE) {
				classesBuilder.add(NODE_CLASS);
			}
			if (ProblemUtil.isIndividualNode(node)) {
				classesBuilder.add(INDIVIDUAL_NODE_CLASS);
			}
			if (ProblemUtil.isNewNode(node)) {
				classesBuilder.add(NEW_NODE_CLASS);
			}
		}
		List<String> classes = classesBuilder.build();
		return classes.toArray(new String[0]);
	}
}
