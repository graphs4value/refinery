/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.utils;

import com.google.inject.Inject;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.naming.IQualifiedNameConverter;
import org.eclipse.xtext.naming.IQualifiedNameProvider;
import tools.refinery.language.model.problem.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

class SymbolCollector {
	@Inject
	private IQualifiedNameProvider qualifiedNameProvider;

	@Inject
	private IQualifiedNameConverter qualifiedNameConverter;

	@Inject
	private ProblemDesugarer desugarer;

	private BuiltinSymbols builtinSymbols;

	private final Map<Node, NodeInfo> nodes = new LinkedHashMap<>();

	private final Map<Relation, RelationInfo> relations = new LinkedHashMap<>();

	public CollectedSymbols collectSymbols(Problem problem) {
		builtinSymbols = desugarer.getBuiltinSymbols(problem).orElseThrow(() -> new IllegalArgumentException(
				"Problem has no associated built-in library"));
		collectOwnSymbols(builtinSymbols.problem());
		collectOwnSymbols(problem);
		return new CollectedSymbols(nodes, relations);
	}

	public void collectOwnSymbols(Problem problem) {
		collectOwnRelations(problem);
		collectOwnNodes(problem);
		collectOwnAssertions(problem);
	}

	private void collectOwnRelations(Problem problem) {
		for (var statement : problem.getStatements()) {
			if (statement instanceof PredicateDefinition predicateDefinition) {
				collectPredicate(predicateDefinition);
			} else if (statement instanceof ClassDeclaration classDeclaration) {
				collectClass(classDeclaration);
			} else if (statement instanceof EnumDeclaration enumDeclaration) {
				collectEnum(enumDeclaration);
			} else if (statement instanceof RuleDefinition) {
				throw new UnsupportedOperationException("Rules are not currently supported");
			}
		}
	}

	private void collectPredicate(PredicateDefinition predicateDefinition) {
		var predicateKind = predicateDefinition.getKind();
		var info = new RelationInfo(getQualifiedNameString(predicateDefinition),
				ContainmentRole.fromPredicateKind(predicateKind), predicateDefinition.getParameters(), null, null,
				predicateDefinition.getBodies());
		relations.put(predicateDefinition, info);
	}

	private void collectClass(ClassDeclaration classDeclaration) {
		var contained = classDeclaration != builtinSymbols.node();
		var containmentRole = contained ? ContainmentRole.CONTAINED : ContainmentRole.NONE;
		var instanceParameter = ProblemFactory.eINSTANCE.createParameter();
		instanceParameter.setName("instance");
		var classInfo = new RelationInfo(getQualifiedNameString(classDeclaration), containmentRole,
				List.of(instanceParameter), null, null, List.of());
		relations.put(classDeclaration, classInfo);
		collectFeatures(classDeclaration);
	}

	private void collectFeatures(ClassDeclaration classDeclaration) {
		for (var featureDeclaration : classDeclaration.getFeatureDeclarations()) {
			if (featureDeclaration instanceof ReferenceDeclaration referenceDeclaration) {
				collectReference(classDeclaration, referenceDeclaration);
			} else if (featureDeclaration instanceof AttributeDeclaration attributeDeclaration) {
				collectAttribute(classDeclaration, attributeDeclaration);
			} else if (featureDeclaration instanceof FlagDeclaration flagDeclaration) {
				collectFlag(classDeclaration, flagDeclaration);
			} else {
				throw new IllegalArgumentException("Unknown FeatureDeclaration: " + featureDeclaration);
			}
		}
	}

	private void collectReference(ClassDeclaration classDeclaration, ReferenceDeclaration referenceDeclaration) {
		var referenceRole = desugarer.isContainmentReference(referenceDeclaration) ?
				ContainmentRole.CONTAINMENT :
				ContainmentRole.NONE;
		var sourceParameter = ProblemFactory.eINSTANCE.createParameter();
		sourceParameter.setName("source");
		sourceParameter.setParameterType(classDeclaration);
		var targetParameter = ProblemFactory.eINSTANCE.createParameter();
		targetParameter.setName("target");
		var multiplicity = referenceDeclaration.getMultiplicity();
		if (multiplicity == null) {
			var exactMultiplicity = ProblemFactory.eINSTANCE.createExactMultiplicity();
			exactMultiplicity.setExactValue(1);
			multiplicity = exactMultiplicity;
		}
		targetParameter.setParameterType(referenceDeclaration.getReferenceType());
		var referenceInfo = new RelationInfo(getQualifiedNameString(referenceDeclaration), referenceRole,
				List.of(sourceParameter, targetParameter), multiplicity, referenceDeclaration.getOpposite(),
				List.of());
		this.relations.put(referenceDeclaration, referenceInfo);
	}

	private void collectAttribute(ClassDeclaration classDeclaration, AttributeDeclaration attributeDeclaration) {
		// TODO Implement attribute handling.
	}

	private void collectFlag(ClassDeclaration classDeclaration, FlagDeclaration flagDeclaration) {
		var parameter = ProblemFactory.eINSTANCE.createParameter();
		parameter.setName("object");
		parameter.setParameterType(classDeclaration);
		var referenceInfo = new RelationInfo(getQualifiedNameString(flagDeclaration), ContainmentRole.NONE,
				List.of(parameter), null, null, List.of());
		this.relations.put(flagDeclaration, referenceInfo);
	}

	private void collectEnum(EnumDeclaration enumDeclaration) {
		var instanceParameter = ProblemFactory.eINSTANCE.createParameter();
		instanceParameter.setName("instance");
		var info = new RelationInfo(getQualifiedNameString(enumDeclaration), ContainmentRole.NONE,
				List.of(instanceParameter), null, null, List.of());
		this.relations.put(enumDeclaration, info);
	}

	private void collectOwnNodes(Problem problem) {
		for (var statement : problem.getStatements()) {
			if (statement instanceof IndividualDeclaration individualDeclaration) {
				collectIndividuals(individualDeclaration);
			} else if (statement instanceof ClassDeclaration classDeclaration) {
				collectNewNode(classDeclaration);
			} else if (statement instanceof EnumDeclaration enumDeclaration) {
				collectEnumLiterals(enumDeclaration);
			}
		}
		for (var node : problem.getNodes()) {
			addNode(node, false);
		}
	}

	private void collectIndividuals(IndividualDeclaration individualDeclaration) {
		for (var individual : individualDeclaration.getNodes()) {
			addNode(individual, true);
		}
	}

	private void collectNewNode(ClassDeclaration classDeclaration) {
		var newNode = classDeclaration.getNewNode();
		if (newNode != null) {
			addNode(newNode, false);
		}
	}

	private void collectEnumLiterals(EnumDeclaration enumDeclaration) {
		for (var literal : enumDeclaration.getLiterals()) {
			addNode(literal, true);
		}
	}

	private void addNode(Node node, boolean individual) {
		var info = new NodeInfo(getQualifiedNameString(node), individual);
		this.nodes.put(node, info);
	}

	private String getQualifiedNameString(EObject eObject) {
		var qualifiedName = qualifiedNameProvider.getFullyQualifiedName(eObject);
		if (qualifiedName == null) {
			return null;
		}
		return qualifiedNameConverter.toString(qualifiedName);
	}

	private void collectOwnAssertions(Problem problem) {
		for (var statement : problem.getStatements()) {
			if (statement instanceof Assertion assertion) {
				collectAssertion(assertion);
			} else if (statement instanceof PredicateDefinition predicateDefinition) {
				collectPredicateAssertion(predicateDefinition);
			} else if (statement instanceof ClassDeclaration classDeclaration) {
				collectClassAssertion(classDeclaration);
			} else if (statement instanceof EnumDeclaration enumDeclaration) {
				collectEnumAssertions(enumDeclaration);
			}
		}
	}

	private void collectAssertion(Assertion assertion) {
		var relationInfo = this.relations.get(assertion.getRelation());
		if (relationInfo == null) {
			throw new IllegalStateException("Assertion refers to unknown relation");
		}
		if (assertion.getArguments().size() != relationInfo.parameters().size()) {
			// Silently ignoring assertions of invalid arity helps when SymbolCollector is called on an invalid
			// Problem during editing. The errors can still be detected by the Problem validator.
			return;
		}
		relationInfo.assertions().add(assertion);
	}

	private void collectPredicateAssertion(PredicateDefinition predicateDefinition) {
		if (predicateDefinition.getKind() != PredicateKind.ERROR) {
			return;
		}
		int arity = predicateDefinition.getParameters().size();
		addAssertion(predicateDefinition, LogicValue.FALSE, new Node[arity]);
	}

	private void collectClassAssertion(ClassDeclaration classDeclaration) {
		var node = classDeclaration.getNewNode();
		if (node == null) {
			return;
		}
		addAssertion(classDeclaration, LogicValue.TRUE, node);
		addAssertion(builtinSymbols.exists(), LogicValue.UNKNOWN, node);
		addAssertion(builtinSymbols.equals(), LogicValue.UNKNOWN, node, node);
	}

	private void collectEnumAssertions(EnumDeclaration enumDeclaration) {
		for (var literal : enumDeclaration.getLiterals()) {
			addAssertion(enumDeclaration, LogicValue.TRUE, literal);
		}
	}

	private void addAssertion(Relation relation, LogicValue logicValue, Node... nodes) {
		var assertion = ProblemFactory.eINSTANCE.createAssertion();
		assertion.setRelation(relation);
		for (var node : nodes) {
			AssertionArgument argument;
			if (node == null) {
				argument = ProblemFactory.eINSTANCE.createWildcardAssertionArgument();
			} else {
				var nodeArgument = ProblemFactory.eINSTANCE.createNodeAssertionArgument();
				nodeArgument.setNode(node);
				argument = nodeArgument;
			}
			assertion.getArguments().add(argument);
		}
		var value = ProblemFactory.eINSTANCE.createLogicConstant();
		value.setLogicValue(logicValue);
		assertion.setValue(value);
		collectAssertion(assertion);
	}
}
