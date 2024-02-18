/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.resource.state;

import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.linking.impl.LinkingHelper;
import org.eclipse.xtext.naming.IQualifiedNameConverter;
import org.eclipse.xtext.nodemodel.INode;
import org.eclipse.xtext.nodemodel.util.NodeModelUtils;
import org.eclipse.xtext.scoping.IScope;
import org.eclipse.xtext.scoping.IScopeProvider;
import org.eclipse.xtext.scoping.impl.AbstractDeclarativeScopeProvider;
import tools.refinery.language.model.problem.*;
import tools.refinery.language.naming.NamingUtil;

import java.util.List;
import java.util.Set;

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
		}
	}

	protected void collectAssertionNodeNames(Assertion assertion) {
		for (AssertionArgument argument : assertion.getArguments()) {
			if (argument instanceof NodeAssertionArgument) {
				collectNodeNames(argument);
			}
		}
	}

	private void collectNodeNames(EObject eObject) {
		List<INode> nodes = NodeModelUtils.findNodesForFeature(eObject,
				ProblemPackage.Literals.NODE_ASSERTION_ARGUMENT__NODE);
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
