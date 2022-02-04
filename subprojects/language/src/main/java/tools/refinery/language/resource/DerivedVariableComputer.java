package tools.refinery.language.resource;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.xtext.linking.impl.LinkingHelper;
import org.eclipse.xtext.naming.IQualifiedNameConverter;
import org.eclipse.xtext.nodemodel.INode;
import org.eclipse.xtext.nodemodel.util.NodeModelUtils;
import org.eclipse.xtext.scoping.IScope;
import org.eclipse.xtext.scoping.IScopeProvider;
import org.eclipse.xtext.scoping.impl.AbstractDeclarativeScopeProvider;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

import tools.refinery.language.model.problem.Argument;
import tools.refinery.language.model.problem.Atom;
import tools.refinery.language.model.problem.Conjunction;
import tools.refinery.language.model.problem.ExistentialQuantifier;
import tools.refinery.language.model.problem.ImplicitVariable;
import tools.refinery.language.model.problem.Literal;
import tools.refinery.language.model.problem.NegativeLiteral;
import tools.refinery.language.model.problem.Parameter;
import tools.refinery.language.model.problem.ParametricDefinition;
import tools.refinery.language.model.problem.Problem;
import tools.refinery.language.model.problem.ProblemFactory;
import tools.refinery.language.model.problem.ProblemPackage;
import tools.refinery.language.model.problem.Statement;
import tools.refinery.language.model.problem.ValueLiteral;
import tools.refinery.language.model.problem.VariableOrNodeArgument;
import tools.refinery.language.naming.NamingUtil;

@Singleton
public class DerivedVariableComputer {
	@Inject
	private LinkingHelper linkingHelper;

	@Inject
	private IQualifiedNameConverter qualifiedNameConverter;

	@Inject
	@Named(AbstractDeclarativeScopeProvider.NAMED_DELEGATE)
	private IScopeProvider scopeProvider;

	public void installDerivedVariables(Problem problem, Set<String> nodeNames) {
		for (Statement statement : problem.getStatements()) {
			if (statement instanceof ParametricDefinition definition) {
				installDerivedParametricDefinitionState(definition, nodeNames);
			}
		}
	}

	protected void installDerivedParametricDefinitionState(ParametricDefinition definition, Set<String> nodeNames) {
		Set<String> knownVariables = new HashSet<>();
		knownVariables.addAll(nodeNames);
		for (Parameter parameter : definition.getParameters()) {
			String name = parameter.getName();
			if (name != null) {
				knownVariables.add(name);
			}
		}
		for (Conjunction body : definition.getBodies()) {
			installDeriveConjunctionState(body, knownVariables);
		}
	}

	protected void installDeriveConjunctionState(Conjunction conjunction, Set<String> knownVariables) {
		Set<String> newVariables = new HashSet<>();
		for (Literal literal : conjunction.getLiterals()) {
			if (literal instanceof Atom atom) {
				createSigletonVariablesAndCollectVariables(atom, knownVariables, newVariables);
			} else
			if (literal instanceof ValueLiteral valueLiteral) {
				createSigletonVariablesAndCollectVariables(valueLiteral.getAtom(), knownVariables, newVariables);
			}
		}
		createVariables(conjunction, newVariables);
		newVariables.addAll(knownVariables);
		for (Literal literal : conjunction.getLiterals()) {
			if (literal instanceof NegativeLiteral negativeLiteral) {
				installDeriveNegativeLiteralState(negativeLiteral, newVariables);
			}
		}
	}

	protected void installDeriveNegativeLiteralState(NegativeLiteral negativeLiteral, Set<String> knownVariables) {
		Set<String> newVariables = new HashSet<>();
		createSigletonVariablesAndCollectVariables(negativeLiteral.getAtom(), knownVariables, newVariables);
		createVariables(negativeLiteral, newVariables);
	}

	protected void createSigletonVariablesAndCollectVariables(Atom atom, Set<String> knownVariables,
			Set<String> newVariables) {
		for (Argument argument : atom.getArguments()) {
			if (argument instanceof VariableOrNodeArgument variableOrNodeArgument) {
				IScope scope = scopeProvider.getScope(variableOrNodeArgument,
						ProblemPackage.Literals.VARIABLE_OR_NODE_ARGUMENT__VARIABLE_OR_NODE);
				List<INode> nodes = NodeModelUtils.findNodesForFeature(variableOrNodeArgument,
						ProblemPackage.Literals.VARIABLE_OR_NODE_ARGUMENT__VARIABLE_OR_NODE);
				for (INode node : nodes) {
					var variableName = linkingHelper.getCrossRefNodeAsString(node, true);
					var created = tryCreateVariableForArgument(variableOrNodeArgument, variableName, scope,
							knownVariables, newVariables);
					if (created) {
						break;
					}
				}
			}
		}
	}

	protected boolean tryCreateVariableForArgument(VariableOrNodeArgument variableOrNodeArgument, String variableName,
			IScope scope, Set<String> knownVariables, Set<String> newVariables) {
		if (!NamingUtil.isValidId(variableName)) {
			return false;
		}
		var qualifiedName = qualifiedNameConverter.toQualifiedName(variableName);
		if (scope.getSingleElement(qualifiedName) != null) {
			return false;
		}
		if (NamingUtil.isSingletonVariableName(variableName)) {
			createSingletonVariable(variableOrNodeArgument, variableName);
			return true;
		}
		if (!knownVariables.contains(variableName)) {
			newVariables.add(variableName);
			return true;
		}
		return false;
	}

	protected void createVariables(ExistentialQuantifier quantifier, Set<String> newVariables) {
		for (String variableName : newVariables) {
			createVariable(quantifier, variableName);
		}
	}

	protected void createVariable(ExistentialQuantifier quantifier, String variableName) {
		if (NamingUtil.isValidId(variableName)) {
			ImplicitVariable variable = createNamedVariable(variableName);
			quantifier.getImplicitVariables().add(variable);
		}
	}

	protected void createSingletonVariable(VariableOrNodeArgument argument, String variableName) {
		if (NamingUtil.isValidId(variableName)) {
			ImplicitVariable variable = createNamedVariable(variableName);
			argument.setSingletonVariable(variable);
		}
	}

	protected ImplicitVariable createNamedVariable(String variableName) {
		var variable = ProblemFactory.eINSTANCE.createImplicitVariable();
		variable.setName(variableName);
		return variable;
	}

	public void discardDerivedVariables(Problem problem) {
		for (Statement statement : problem.getStatements()) {
			if (statement instanceof ParametricDefinition parametricDefinition) {
				discardParametricDefinitionState(parametricDefinition);
			}
		}
	}

	protected void discardParametricDefinitionState(ParametricDefinition definition) {
		for (Conjunction body : definition.getBodies()) {
			body.getImplicitVariables().clear();
			for (Literal literal : body.getLiterals()) {
				if (literal instanceof Atom atom) {
					discardDerivedAtomState(atom);
				}
				if (literal instanceof NegativeLiteral negativeLiteral) {
					negativeLiteral.getImplicitVariables().clear();
					discardDerivedAtomState(negativeLiteral.getAtom());
				}
			}
		}
	}

	protected void discardDerivedAtomState(Atom atom) {
		if (atom == null) {
			return;
		}
		for (Argument argument : atom.getArguments()) {
			if (argument instanceof VariableOrNodeArgument variableOrNodeArgument) {
				variableOrNodeArgument.setSingletonVariable(null);
			}
		}
	}
}
