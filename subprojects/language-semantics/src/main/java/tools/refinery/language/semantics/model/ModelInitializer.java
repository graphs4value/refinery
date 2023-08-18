/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.semantics.model;

import com.google.inject.Inject;
import org.eclipse.collections.api.factory.primitive.ObjectIntMaps;
import org.eclipse.collections.api.map.primitive.MutableObjectIntMap;
import tools.refinery.language.model.problem.*;
import tools.refinery.language.semantics.model.internal.DecisionTree;
import tools.refinery.language.utils.BuiltinSymbols;
import tools.refinery.language.utils.ProblemDesugarer;
import tools.refinery.language.utils.ProblemUtil;
import tools.refinery.store.model.ModelStoreBuilder;
import tools.refinery.store.query.Constraint;
import tools.refinery.store.query.dnf.Query;
import tools.refinery.store.query.dnf.RelationalQuery;
import tools.refinery.store.query.literal.*;
import tools.refinery.store.query.term.NodeVariable;
import tools.refinery.store.query.term.Variable;
import tools.refinery.store.reasoning.ReasoningAdapter;
import tools.refinery.store.reasoning.representation.PartialRelation;
import tools.refinery.store.reasoning.seed.ModelSeed;
import tools.refinery.store.reasoning.seed.Seed;
import tools.refinery.store.reasoning.translator.containment.ContainmentHierarchyTranslator;
import tools.refinery.store.reasoning.translator.metamodel.Metamodel;
import tools.refinery.store.reasoning.translator.metamodel.MetamodelBuilder;
import tools.refinery.store.reasoning.translator.metamodel.MetamodelTranslator;
import tools.refinery.store.reasoning.translator.multiobject.MultiObjectTranslator;
import tools.refinery.store.reasoning.translator.multiplicity.ConstrainedMultiplicity;
import tools.refinery.store.reasoning.translator.multiplicity.Multiplicity;
import tools.refinery.store.reasoning.translator.multiplicity.UnconstrainedMultiplicity;
import tools.refinery.store.reasoning.translator.predicate.PredicateTranslator;
import tools.refinery.store.representation.TruthValue;
import tools.refinery.store.representation.cardinality.CardinalityInterval;
import tools.refinery.store.representation.cardinality.CardinalityIntervals;
import tools.refinery.store.representation.cardinality.UpperCardinalities;
import tools.refinery.store.tuple.Tuple;

import java.util.*;

public class ModelInitializer {
	@Inject
	private ProblemDesugarer desugarer;

	@Inject
	private SemanticsUtils semanticsUtils;

	private Problem problem;

	private ModelStoreBuilder storeBuilder;

	private BuiltinSymbols builtinSymbols;

	private PartialRelation nodeRelation;

	private final MutableObjectIntMap<Node> nodeTrace = ObjectIntMaps.mutable.empty();

	private final Map<Relation, RelationInfo> relationInfoMap = new LinkedHashMap<>();

	private final Map<PartialRelation, RelationInfo> partialRelationInfoMap = new LinkedHashMap<>();

	private Map<Relation, PartialRelation> relationTrace;

	private final MetamodelBuilder metamodelBuilder = Metamodel.builder();

	private Metamodel metamodel;

	private ModelSeed modelSeed;

	public int getNodeCount() {
		return nodeTrace.size();
	}

	public MutableObjectIntMap<Node> getNodeTrace() {
		return nodeTrace;
	}

	public Map<Relation, PartialRelation> getRelationTrace() {
		return relationTrace;
	}

	public ModelSeed createModel(Problem problem, ModelStoreBuilder storeBuilder) {
		this.problem = problem;
		this.storeBuilder = storeBuilder;
		builtinSymbols = desugarer.getBuiltinSymbols(problem).orElseThrow(() -> new IllegalArgumentException(
				"Problem has no builtin library"));
		var nodeInfo = collectPartialRelation(builtinSymbols.node(), 1, TruthValue.TRUE, TruthValue.TRUE);
		nodeRelation = nodeInfo.partialRelation();
		metamodelBuilder.type(nodeRelation);
		putRelationInfo(builtinSymbols.exists(), new RelationInfo(ReasoningAdapter.EXISTS_SYMBOL, null,
				TruthValue.TRUE));
		putRelationInfo(builtinSymbols.equals(), new RelationInfo(ReasoningAdapter.EQUALS_SYMBOL,
				(TruthValue) null,
				null));
		putRelationInfo(builtinSymbols.contained(), new RelationInfo(ContainmentHierarchyTranslator.CONTAINED_SYMBOL,
				null, TruthValue.UNKNOWN));
		putRelationInfo(builtinSymbols.contains(), new RelationInfo(ContainmentHierarchyTranslator.CONTAINS_SYMBOL,
				null, TruthValue.UNKNOWN));
		putRelationInfo(builtinSymbols.invalidNumberOfContainers(),
				new RelationInfo(ContainmentHierarchyTranslator.INVALID_NUMBER_OF_CONTAINERS, TruthValue.FALSE,
						TruthValue.FALSE));
		collectNodes();
		collectPartialSymbols();
		collectMetamodel();
		metamodel = metamodelBuilder.build();
		collectAssertions();
		storeBuilder.with(new MultiObjectTranslator());
		storeBuilder.with(new MetamodelTranslator(metamodel));
		relationTrace = new LinkedHashMap<>(relationInfoMap.size());
		int nodeCount = getNodeCount();
		var modelSeedBuilder = ModelSeed.builder(nodeCount);
		for (var entry : relationInfoMap.entrySet()) {
			var relation = entry.getKey();
			var info = entry.getValue();
			var partialRelation = info.partialRelation();
			relationTrace.put(relation, partialRelation);
			modelSeedBuilder.seed(partialRelation, info.toSeed(nodeCount));
		}
		modelSeed = modelSeedBuilder.build();
		collectPredicates();
		return modelSeed;
	}

	private void collectNodes() {
		for (var statement : problem.getStatements()) {
			if (statement instanceof IndividualDeclaration individualDeclaration) {
				for (var individual : individualDeclaration.getNodes()) {
					collectNode(individual);
				}
			} else if (statement instanceof ClassDeclaration classDeclaration) {
				var newNode = classDeclaration.getNewNode();
				if (newNode != null) {
					collectNode(newNode);
				}
			} else if (statement instanceof EnumDeclaration enumDeclaration) {
				for (var literal : enumDeclaration.getLiterals()) {
					collectNode(literal);
				}
			}
		}
		for (var node : problem.getNodes()) {
			collectNode(node);
		}
	}

	private void collectNode(Node node) {
		nodeTrace.getIfAbsentPut(node, this::getNodeCount);
	}

	private void collectPartialSymbols() {
		for (var statement : problem.getStatements()) {
			if (statement instanceof ClassDeclaration classDeclaration) {
				collectClassDeclarationSymbols(classDeclaration);
			} else if (statement instanceof EnumDeclaration enumDeclaration) {
				collectPartialRelation(enumDeclaration, 1, null, TruthValue.FALSE);
			} else if (statement instanceof PredicateDefinition predicateDefinition) {
				collectPredicateDefinitionSymbol(predicateDefinition);
			}
		}
	}

	private void collectClassDeclarationSymbols(ClassDeclaration classDeclaration) {
		collectPartialRelation(classDeclaration, 1, null, TruthValue.UNKNOWN);
		for (var featureDeclaration : classDeclaration.getFeatureDeclarations()) {
			if (featureDeclaration instanceof ReferenceDeclaration referenceDeclaration) {
				collectPartialRelation(referenceDeclaration, 2, null, TruthValue.UNKNOWN);
				var invalidMultiplicityConstraint = referenceDeclaration.getInvalidMultiplicity();
				if (invalidMultiplicityConstraint != null) {
					collectPartialRelation(invalidMultiplicityConstraint, 1, TruthValue.FALSE, TruthValue.FALSE);
				}
			} else {
				throw new IllegalArgumentException("Unknown feature declaration: " + featureDeclaration);
			}
		}
	}

	private void collectPredicateDefinitionSymbol(PredicateDefinition predicateDefinition) {
		int arity = predicateDefinition.getParameters().size();
		if (predicateDefinition.isError()) {
			collectPartialRelation(predicateDefinition, arity, TruthValue.FALSE, TruthValue.FALSE);
		} else {
			collectPartialRelation(predicateDefinition, arity, null, TruthValue.UNKNOWN);
		}
	}

	private void putRelationInfo(Relation relation, RelationInfo info) {
		relationInfoMap.put(relation, info);
		partialRelationInfoMap.put(info.partialRelation(), info);
	}

	private RelationInfo collectPartialRelation(Relation relation, int arity, TruthValue value,
												TruthValue defaultValue) {
		return relationInfoMap.computeIfAbsent(relation, key -> {
			var name = getName(relation);
			var info = new RelationInfo(name, arity, value, defaultValue);
			partialRelationInfoMap.put(info.partialRelation(), info);
			return info;
		});
	}

	private String getName(Relation relation) {
		return semanticsUtils.getName(relation).orElseGet(() -> "#" + relationInfoMap.size());
	}

	private void collectMetamodel() {
		for (var statement : problem.getStatements()) {
			if (statement instanceof ClassDeclaration classDeclaration) {
				collectClassDeclarationMetamodel(classDeclaration);
			} else if (statement instanceof EnumDeclaration enumDeclaration) {
				collectEnumMetamodel(enumDeclaration);
			}
		}
	}

	private void collectEnumMetamodel(EnumDeclaration enumDeclaration) {
		metamodelBuilder.type(getPartialRelation(enumDeclaration), nodeRelation);
	}

	private void collectClassDeclarationMetamodel(ClassDeclaration classDeclaration) {
		var superTypes = classDeclaration.getSuperTypes();
		var partialSuperTypes = new ArrayList<PartialRelation>(superTypes.size() + 1);
		partialSuperTypes.add(nodeRelation);
		for (var superType : superTypes) {
			partialSuperTypes.add(getPartialRelation(superType));
		}
		metamodelBuilder.type(getPartialRelation(classDeclaration), classDeclaration.isAbstract(),
				partialSuperTypes);
		for (var featureDeclaration : classDeclaration.getFeatureDeclarations()) {
			if (featureDeclaration instanceof ReferenceDeclaration referenceDeclaration) {
				collectReferenceDeclarationMetamodel(classDeclaration, referenceDeclaration);
			} else {
				throw new IllegalArgumentException("Unknown feature declaration: " + featureDeclaration);
			}
		}
	}

	private void collectReferenceDeclarationMetamodel(ClassDeclaration classDeclaration,
													  ReferenceDeclaration referenceDeclaration) {
		var relation = getPartialRelation(referenceDeclaration);
		var source = getPartialRelation(classDeclaration);
		var target = getPartialRelation(referenceDeclaration.getReferenceType());
		boolean containment = referenceDeclaration.getKind() == ReferenceKind.CONTAINMENT;
		var opposite = referenceDeclaration.getOpposite();
		PartialRelation oppositeRelation = null;
		if (opposite != null) {
			oppositeRelation = getPartialRelation(opposite);
		}
		var multiplicity = getMultiplicityConstraint(referenceDeclaration);
		metamodelBuilder.reference(relation, source, containment, multiplicity, target, oppositeRelation);
	}

	private Multiplicity getMultiplicityConstraint(ReferenceDeclaration referenceDeclaration) {
		if (!ProblemUtil.hasMultiplicityConstraint(referenceDeclaration)) {
			return UnconstrainedMultiplicity.INSTANCE;
		}
		var problemMultiplicity = referenceDeclaration.getMultiplicity();
		CardinalityInterval interval;
		if (problemMultiplicity == null) {
			interval = CardinalityIntervals.LONE;
		} else if (problemMultiplicity instanceof ExactMultiplicity exactMultiplicity) {
			interval = CardinalityIntervals.exactly(exactMultiplicity.getExactValue());
		} else if (problemMultiplicity instanceof RangeMultiplicity rangeMultiplicity) {
			var upperBound = rangeMultiplicity.getUpperBound();
			interval = CardinalityIntervals.between(rangeMultiplicity.getLowerBound(),
					upperBound < 0 ? UpperCardinalities.UNBOUNDED : UpperCardinalities.atMost(upperBound));
		} else {
			throw new IllegalArgumentException("Unknown multiplicity: " + problemMultiplicity);
		}
		var constraint = getRelationInfo(referenceDeclaration.getInvalidMultiplicity()).partialRelation();
		return ConstrainedMultiplicity.of(interval, constraint);
	}


	private void collectAssertions() {
		for (var statement : problem.getStatements()) {
			if (statement instanceof ClassDeclaration classDeclaration) {
				collectClassDeclarationAssertions(classDeclaration);
			} else if (statement instanceof EnumDeclaration enumDeclaration) {
				collectEnumAssertions(enumDeclaration);
			} else if (statement instanceof IndividualDeclaration individualDeclaration) {
				for (var individual : individualDeclaration.getNodes()) {
					collectIndividualAssertions(individual);
				}
			} else if (statement instanceof Assertion assertion) {
				collectAssertion(assertion);
			}
		}
	}

	private void collectClassDeclarationAssertions(ClassDeclaration classDeclaration) {
		var newNode = classDeclaration.getNewNode();
		if (newNode == null) {
			return;
		}
		var newNodeId = getNodeId(newNode);
		collectCardinalityAssertions(newNodeId, TruthValue.UNKNOWN);
		var tuple = Tuple.of(newNodeId);
		mergeValue(classDeclaration, tuple, TruthValue.TRUE);
		var typeInfo = metamodel.typeHierarchy().getAnalysisResult(getPartialRelation(classDeclaration));
		for (var subType : typeInfo.getDirectSubtypes()) {
			partialRelationInfoMap.get(subType).assertions().mergeValue(tuple, TruthValue.FALSE);
		}
	}

	private void collectEnumAssertions(EnumDeclaration enumDeclaration) {
		var overlay = new DecisionTree(1, null);
		for (var literal : enumDeclaration.getLiterals()) {
			collectIndividualAssertions(literal);
			var nodeId = getNodeId(literal);
			overlay.mergeValue(Tuple.of(nodeId), TruthValue.TRUE);
		}
		var info = getRelationInfo(enumDeclaration);
		info.assertions().overwriteValues(overlay);
	}

	private void collectIndividualAssertions(Node node) {
		var nodeId = getNodeId(node);
		collectCardinalityAssertions(nodeId, TruthValue.TRUE);
	}

	private void collectCardinalityAssertions(int nodeId, TruthValue value) {
		mergeValue(builtinSymbols.exists(), Tuple.of(nodeId), value);
		mergeValue(builtinSymbols.equals(), Tuple.of(nodeId, nodeId), value);
	}

	private void collectAssertion(Assertion assertion) {
		var relation = assertion.getRelation();
		var tuple = getTuple(assertion);
		var value = getTruthValue(assertion.getValue());
		if (assertion.isDefault()) {
			mergeDefaultValue(relation, tuple, value);
		} else {
			mergeValue(relation, tuple, value);
		}
	}

	private void mergeValue(Relation relation, Tuple key, TruthValue value) {
		getRelationInfo(relation).assertions().mergeValue(key, value);
	}

	private void mergeDefaultValue(Relation relation, Tuple key, TruthValue value) {
		getRelationInfo(relation).defaultAssertions().mergeValue(key, value);
	}

	private RelationInfo getRelationInfo(Relation relation) {
		var info = relationInfoMap.get(relation);
		if (info == null) {
			throw new IllegalArgumentException("Unknown relation: " + relation);
		}
		return info;
	}

	private PartialRelation getPartialRelation(Relation relation) {
		return getRelationInfo(relation).partialRelation();
	}

	private int getNodeId(Node node) {
		return nodeTrace.getOrThrow(node);
	}

	private Tuple getTuple(Assertion assertion) {
		var arguments = assertion.getArguments();
		int arity = arguments.size();
		var nodes = new int[arity];
		for (int i = 0; i < arity; i++) {
			var argument = arguments.get(i);
			if (argument instanceof NodeAssertionArgument nodeArgument) {
				nodes[i] = getNodeId(nodeArgument.getNode());
			} else if (argument instanceof WildcardAssertionArgument) {
				nodes[i] = -1;
			} else {
				throw new IllegalArgumentException("Unknown assertion argument: " + argument);
			}
		}
		return Tuple.of(nodes);
	}

	private static TruthValue getTruthValue(Expr expr) {
		if (!(expr instanceof LogicConstant logicAssertionValue)) {
			return TruthValue.ERROR;
		}
		return switch (logicAssertionValue.getLogicValue()) {
			case TRUE -> TruthValue.TRUE;
			case FALSE -> TruthValue.FALSE;
			case UNKNOWN -> TruthValue.UNKNOWN;
			case ERROR -> TruthValue.ERROR;
		};
	}

	private void collectPredicates() {
		for (var statement : problem.getStatements()) {
			if (statement instanceof PredicateDefinition predicateDefinition) {
				collectPredicateDefinition(predicateDefinition);
			}
		}
	}

	private void collectPredicateDefinition(PredicateDefinition predicateDefinition) {
		var partialRelation = getPartialRelation(predicateDefinition);
		var query = toQuery(partialRelation.name(), predicateDefinition);
		boolean mutable;
		TruthValue defaultValue;
		if (predicateDefinition.isError()) {
			mutable = false;
			defaultValue = TruthValue.FALSE;
		} else {
			var seed = modelSeed.getSeed(partialRelation);
			defaultValue = seed.reducedValue() == TruthValue.FALSE ? TruthValue.FALSE : TruthValue.UNKNOWN;
			var cursor = seed.getCursor(defaultValue, getNodeCount());
			// The symbol should be mutable if there is at least one non-default entry in the seed.
			mutable = cursor.move();
		}
		var translator = new PredicateTranslator(partialRelation, query, mutable, defaultValue);
		storeBuilder.with(translator);
	}

	private RelationalQuery toQuery(String name, PredicateDefinition predicateDefinition) {
		var problemParameters = predicateDefinition.getParameters();
		int arity = problemParameters.size();
		var parameters = new NodeVariable[arity];
		var parameterMap = new HashMap<tools.refinery.language.model.problem.Variable, Variable>(arity);
		var commonLiterals = new ArrayList<Literal>();
		for (int i = 0; i < arity; i++) {
			var problemParameter = problemParameters.get(i);
			var parameter = Variable.of(problemParameter.getName());
			parameters[i] = parameter;
			parameterMap.put(problemParameter, parameter);
			var parameterType = problemParameter.getParameterType();
			if (parameterType != null) {
				var partialType = getPartialRelation(parameterType);
				commonLiterals.add(partialType.call(parameter));
			}
		}
		var builder = Query.builder(name).parameters(parameters);
		for (var body : predicateDefinition.getBodies()) {
			var localScope = extendScope(parameterMap, body.getImplicitVariables());
			var problemLiterals = body.getLiterals();
			var literals = new ArrayList<Literal>(commonLiterals);
			for (var problemLiteral : problemLiterals) {
				toLiterals(problemLiteral, localScope, literals);
			}
			builder.clause(literals);
		}
		return builder.build();
	}

	private Map<tools.refinery.language.model.problem.Variable, Variable> extendScope(
			Map<tools.refinery.language.model.problem.Variable, Variable> existing,
			Collection<? extends tools.refinery.language.model.problem.Variable> newVariables) {
		if (newVariables.isEmpty()) {
			return existing;
		}
		int localScopeSize = existing.size() + newVariables.size();
		var localScope = new HashMap<tools.refinery.language.model.problem.Variable, Variable>(localScopeSize);
		localScope.putAll(existing);
		for (var newVariable : newVariables) {
			localScope.put(newVariable, Variable.of(newVariable.getName()));
		}
		return localScope;
	}

	private void toLiterals(Expr expr, Map<tools.refinery.language.model.problem.Variable, Variable> localScope,
							List<Literal> literals) {
		if (expr instanceof LogicConstant logicConstant) {
			switch (logicConstant.getLogicValue()) {
				case TRUE -> literals.add(BooleanLiteral.TRUE);
				case FALSE -> literals.add(BooleanLiteral.FALSE);
				default -> throw new IllegalArgumentException("Unsupported literal: " + expr);
			}
		} else if (expr instanceof Atom atom) {
			var target = getPartialRelation(atom.getRelation());
			var polarity = atom.isTransitiveClosure() ? CallPolarity.TRANSITIVE : CallPolarity.POSITIVE;
			var argumentList = toArgumentList(atom.getArguments(), localScope, literals);
			literals.add(target.call(polarity, argumentList));
		} else if (expr instanceof NegationExpr negationExpr) {
			var body = negationExpr.getBody();
			if (!(body instanceof Atom atom)) {
				throw new IllegalArgumentException("Cannot negate literal: " + body);
			}
			var target = getPartialRelation(atom.getRelation());
			Constraint constraint;
			if (atom.isTransitiveClosure()) {
				constraint = Query.of(target.name() + "#transitive", (builder, p1, p2) -> builder.clause(
						target.callTransitive(p1, p2)
				)).getDnf();
			} else {
				constraint = target;
			}
			var negatedScope = extendScope(localScope, negationExpr.getImplicitVariables());
			var argumentList = toArgumentList(atom.getArguments(), negatedScope, literals);
			literals.add(constraint.call(CallPolarity.NEGATIVE, argumentList));
		} else if (expr instanceof ComparisonExpr comparisonExpr) {
			var argumentList = toArgumentList(List.of(comparisonExpr.getLeft(), comparisonExpr.getRight()),
					localScope, literals);
			boolean positive = switch (comparisonExpr.getOp()) {
				case EQ -> true;
				case NOT_EQ -> false;
				default -> throw new IllegalArgumentException("Unsupported operator: " + comparisonExpr.getOp());
			};
			literals.add(new EquivalenceLiteral(positive, argumentList.get(0), argumentList.get(1)));
		} else {
			throw new IllegalArgumentException("Unsupported literal: " + expr);
		}
	}

	private List<Variable> toArgumentList(
			List<Expr> expressions, Map<tools.refinery.language.model.problem.Variable, Variable> localScope,
			List<Literal> literals) {
		var argumentList = new ArrayList<Variable>(expressions.size());
		for (var expr : expressions) {
			if (!(expr instanceof VariableOrNodeExpr variableOrNodeExpr)) {
				throw new IllegalArgumentException("Unsupported argument: " + expr);
			}
			var variableOrNode = variableOrNodeExpr.getVariableOrNode();
			if (variableOrNode instanceof Node node) {
				int nodeId = getNodeId(node);
				var tempVariable = Variable.of(semanticsUtils.getName(node).orElse("_" + nodeId));
				literals.add(new ConstantLiteral(tempVariable, nodeId));
				argumentList.add(tempVariable);
			} else if (variableOrNode instanceof tools.refinery.language.model.problem.Variable problemVariable) {
				if (variableOrNodeExpr.getSingletonVariable() == problemVariable) {
					argumentList.add(Variable.of(problemVariable.getName()));
				} else {
					var variable = localScope.get(problemVariable);
					if (variable == null) {
						throw new IllegalArgumentException("Unknown variable: " + problemVariable.getName());
					}
					argumentList.add(variable);
				}
			}
		}
		return argumentList;
	}

	private record RelationInfo(PartialRelation partialRelation, DecisionTree assertions,
								DecisionTree defaultAssertions) {
		public RelationInfo(String name, int arity, TruthValue value, TruthValue defaultValue) {
			this(new PartialRelation(name, arity), value, defaultValue);
		}

		public RelationInfo(PartialRelation partialRelation, TruthValue value, TruthValue defaultValue) {
			this(partialRelation, new DecisionTree(partialRelation.arity(), value),
					new DecisionTree(partialRelation.arity(), defaultValue));
		}

		public Seed<TruthValue> toSeed(int nodeCount) {
			defaultAssertions.overwriteValues(assertions);
			if (partialRelation.equals(ReasoningAdapter.EQUALS_SYMBOL)) {
				for (int i = 0; i < nodeCount; i++) {
					defaultAssertions.setIfMissing(Tuple.of(i, i), TruthValue.TRUE);
				}
				defaultAssertions.setAllMissing(TruthValue.FALSE);
			}
			return defaultAssertions;
		}
	}
}
