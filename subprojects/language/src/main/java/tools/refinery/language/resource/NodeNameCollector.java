package tools.refinery.language.resource;

import java.util.List;
import java.util.Set;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.xtext.linking.impl.LinkingHelper;
import org.eclipse.xtext.naming.IQualifiedNameConverter;
import org.eclipse.xtext.nodemodel.INode;
import org.eclipse.xtext.nodemodel.util.NodeModelUtils;
import org.eclipse.xtext.scoping.IScope;
import org.eclipse.xtext.scoping.IScopeProvider;
import org.eclipse.xtext.scoping.impl.AbstractDeclarativeScopeProvider;

import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;
import com.google.inject.name.Named;

import tools.refinery.language.model.problem.Assertion;
import tools.refinery.language.model.problem.AssertionArgument;
import tools.refinery.language.model.problem.NodeAssertionArgument;
import tools.refinery.language.model.problem.NodeValueAssertion;
import tools.refinery.language.model.problem.Problem;
import tools.refinery.language.model.problem.ProblemPackage;
import tools.refinery.language.model.problem.Statement;
import tools.refinery.language.naming.NamingUtil;

public class NodeNameCollector {
	@Inject
	private LinkingHelper linkingHelper;

	@Inject
	private IQualifiedNameConverter qualifiedNameConverter;

	@Inject
	@Named(AbstractDeclarativeScopeProvider.NAMED_DELEGATE)
	private IScopeProvider scopeProvider;

	private final ImmutableSet.Builder<String> nodeNames = ImmutableSet.builder();

	private IScope nodeScope;

	public Set<String> getNodeNames() {
		return nodeNames.build();
	}

	public void collectNodeNames(Problem problem) {
		nodeScope = scopeProvider.getScope(problem, ProblemPackage.Literals.NODE_ASSERTION_ARGUMENT__NODE);
		for (Statement statement : problem.getStatements()) {
			collectStatementNodeNames(statement);
		}
	}

	protected void collectStatementNodeNames(Statement statement) {
		if (statement instanceof Assertion assertion) {
			collectAssertionNodeNames(assertion);
		} else if (statement instanceof NodeValueAssertion nodeValueAssertion) {
			collectNodeValueAssertionNodeNames(nodeValueAssertion);
		}
	}

	protected void collectAssertionNodeNames(Assertion assertion) {
		for (AssertionArgument argument : assertion.getArguments()) {
			if (argument instanceof NodeAssertionArgument) {
				collectNodeNames(argument, ProblemPackage.Literals.NODE_ASSERTION_ARGUMENT__NODE);
			}
		}
	}

	protected void collectNodeValueAssertionNodeNames(NodeValueAssertion nodeValueAssertion) {
		collectNodeNames(nodeValueAssertion, ProblemPackage.Literals.NODE_VALUE_ASSERTION__NODE);
	}

	private void collectNodeNames(EObject eObject, EStructuralFeature feature) {
		List<INode> nodes = NodeModelUtils.findNodesForFeature(eObject, feature);
		for (INode node : nodes) {
			var nodeName = linkingHelper.getCrossRefNodeAsString(node, true);
			if (!NamingUtil.isValidId(nodeName)) {
				continue;
			}
			var qualifiedName = qualifiedNameConverter.toQualifiedName(nodeName);
			if (nodeScope.getSingleElement(qualifiedName) == null) {
				nodeNames.add(nodeName);
			}
		}
	}
}