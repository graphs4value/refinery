/*
 * SPDX-FileCopyrightText: 2021-2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.resource.state;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.linking.impl.LinkingHelper;
import org.eclipse.xtext.naming.IQualifiedNameConverter;
import org.eclipse.xtext.naming.QualifiedName;
import org.eclipse.xtext.nodemodel.INode;
import org.eclipse.xtext.nodemodel.util.NodeModelUtils;
import org.eclipse.xtext.scoping.IScope;
import org.eclipse.xtext.scoping.IScopeProvider;
import tools.refinery.language.model.problem.*;
import tools.refinery.language.naming.NamingUtil;
import tools.refinery.language.resource.ProblemResourceDescriptionStrategy;

import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

class ImplicitVariableScope {
	private final boolean shouldAddVariables;
	private final EObject root;
	private final ExistentialQuantifier quantifier;
	private final ImplicitVariableScope parent;
	private Set<String> knownVariables;

	public ImplicitVariableScope(ExistentialQuantifier quantifier, ImplicitVariableScope parent,
								 boolean shouldAddVariables) {
		this.shouldAddVariables = isShouldAddVariables(quantifier, shouldAddVariables);
		this.root = quantifier;
		this.quantifier = quantifier;
		this.parent = parent;
		this.knownVariables = null;
	}

	public ImplicitVariableScope(EObject root, ExistentialQuantifier quantifier, Set<String> knownVariables,
								 boolean shouldAddVariables) {
		this.shouldAddVariables = isShouldAddVariables(quantifier, shouldAddVariables);
		this.root = root;
		this.quantifier = quantifier;
		this.parent = null;
		this.knownVariables = new HashSet<>(knownVariables);
	}

	private static boolean isShouldAddVariables(ExistentialQuantifier quantifier, boolean shouldAddVariables) {
		return shouldAddVariables || quantifier instanceof AggregationExpr;
	}

	public ImplicitVariableScope(EObject root, ExistentialQuantifier quantifier, Set<String> knownVariables) {
		this(root, quantifier, knownVariables, true);
	}

	public ImplicitVariableScope(ExistentialQuantifier root, Set<String> knownVariables) {
		this(root, root, knownVariables);
	}

	public void createVariables(IScopeProvider scopeProvider, LinkingHelper linkingHelper,
								IQualifiedNameConverter qualifiedNameConverter,
								Deque<ImplicitVariableScope> scopeQueue) {
		initializeKnownVariables();
		if (root instanceof AggregationExpr aggregationExpr) {
			// Aggregation expressions for functions (i.e., not literals) can only introduce new variables in their
			// condition expression, but not in their value expression.
			var condition = aggregationExpr.getCondition();
			if (condition != null) {
				processChild(condition, scopeProvider, linkingHelper, qualifiedNameConverter, scopeQueue);
				walkChildren(condition, scopeProvider, linkingHelper, qualifiedNameConverter, scopeQueue);
			}
			walkAggregatorChildren(scopeQueue, aggregationExpr.getValue());
			return;
		}
		if (root instanceof Case match) {
			var condition = match.getCondition();
			if (condition != null) {
				walkChildren(condition, scopeProvider, linkingHelper, qualifiedNameConverter, scopeQueue);
			}
			walkAggregatorChildren(scopeQueue, match.getValue());
			return;
		}
		processEObject(root, scopeProvider, linkingHelper, qualifiedNameConverter);
		walkChildren(root, scopeProvider, linkingHelper, qualifiedNameConverter, scopeQueue);
		return;
	}


	private void walkChildren(
			EObject parent, IScopeProvider scopeProvider, LinkingHelper linkingHelper,
			IQualifiedNameConverter qualifiedNameConverter, Deque<ImplicitVariableScope> scopeQueue) {
		var treeIterator = parent.eAllContents();
		while (treeIterator.hasNext()) {
			var child = treeIterator.next();
			if (processChild(child, scopeProvider, linkingHelper, qualifiedNameConverter, scopeQueue)) {
				treeIterator.prune();
			}
		}
	}

	private boolean processChild(
			EObject child, IScopeProvider scopeProvider, LinkingHelper linkingHelper,
			IQualifiedNameConverter qualifiedNameConverter, Deque<ImplicitVariableScope> scopeQueue) {
		if (child instanceof ExistentialQuantifier nestedQuantifier) {
			scopeQueue.addLast(new ImplicitVariableScope(nestedQuantifier, this, shouldAddVariables));
			return true;
		}
		processEObject(child, scopeProvider, linkingHelper, qualifiedNameConverter);
		return false;
	}

	private void walkAggregatorChildren(Deque<ImplicitVariableScope> scopeQueue, Expr value) {
		if (value != null && !processAggregatorChild(value, scopeQueue)) {
			var treeIterator = value.eAllContents();
			while (treeIterator.hasNext()) {
				var child = treeIterator.next();
				if (processAggregatorChild(child, scopeQueue)) {
					treeIterator.prune();
				}
			}
		}
	}

	private boolean processAggregatorChild(
			EObject child, Deque<ImplicitVariableScope> scopeQueue) {
		if (child instanceof AggregationExpr aggregationExpr) {
			scopeQueue.addLast(new ImplicitVariableScope(aggregationExpr, this, shouldAddVariables));
			return true;
		}
		return false;
	}

	private void initializeKnownVariables() {
		boolean hasKnownVariables = knownVariables != null;
		boolean hasParent = parent != null;
		if ((hasKnownVariables && hasParent) || (!hasKnownVariables && !hasParent)) {
			throw new IllegalStateException("Either known variables or parent must be provided, but not both");
		}
		if (!hasKnownVariables) {
			if (parent.knownVariables == null) {
				throw new IllegalStateException("Parent scope must be processed before current scope");
			}
			knownVariables = new HashSet<>(parent.knownVariables);
		}
	}

	private void processEObject(EObject eObject, IScopeProvider scopeProvider, LinkingHelper linkingHelper,
								IQualifiedNameConverter qualifiedNameConverter) {
		if (!shouldAddVariables || !(eObject instanceof VariableOrNodeExpr variableOrNodeExpr)) {
			return;
		}
		IScope scope = scopeProvider.getScope(variableOrNodeExpr,
				ProblemPackage.Literals.VARIABLE_OR_NODE_EXPR__VARIABLE_OR_NODE);
		List<INode> nodes = NodeModelUtils.findNodesForFeature(variableOrNodeExpr,
				ProblemPackage.Literals.VARIABLE_OR_NODE_EXPR__ELEMENT);
		for (INode node : nodes) {
			var variableName = linkingHelper.getCrossRefNodeAsString(node, true);
			var created = tryCreateVariableForArgument(variableOrNodeExpr, variableName, qualifiedNameConverter,
					scope);
			if (created) {
				break;
			}
		}
	}

	protected boolean tryCreateVariableForArgument(VariableOrNodeExpr variableOrNodeExpr, String crossRefString,
												   IQualifiedNameConverter qualifiedNameConverter, IScope scope) {
		if (!NamingUtil.isValidId(crossRefString)) {
			return false;
		}
		QualifiedName qualifiedName;
		try {
			qualifiedName = qualifiedNameConverter.toQualifiedName(crossRefString);
		} catch (IllegalArgumentException e) {
			return false;
		}
		if (qualifiedName.getSegmentCount() != 1) {
			return false;
		}
		var variableName = qualifiedName.getFirstSegment();
		if (NamingUtil.isSingletonVariableName(crossRefString)) {
			createSingletonVariable(variableOrNodeExpr, variableName);
			return true;
		}
		var element = scope.getSingleElement(qualifiedName);
		if (element != null && ProblemResourceDescriptionStrategy.ATOM_TRUE.equals(
				element.getUserData(ProblemResourceDescriptionStrategy.ATOM))) {
			return false;
		}
		if (!knownVariables.contains(variableName)) {
			createVariable(variableName);
			return true;
		}
		return false;
	}

	protected void createVariable(String variableName) {
		knownVariables.add(variableName);
		ImplicitVariable variable = createNamedVariable(variableName);
		quantifier.getImplicitVariables().add(variable);
	}

	protected void createSingletonVariable(VariableOrNodeExpr variableOrNodeExpr, String variableName) {
		ImplicitVariable variable = createNamedVariable(variableName);
		variableOrNodeExpr.setSingletonVariable(variable);
	}

	protected ImplicitVariable createNamedVariable(String variableName) {
		var variable = ProblemFactory.eINSTANCE.createImplicitVariable();
		variable.setName(variableName);
		return variable;
	}
}
