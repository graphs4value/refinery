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
import tools.refinery.language.semantics.model.internal.MutableSeed;
import tools.refinery.language.utils.BuiltinSymbols;
import tools.refinery.language.utils.ProblemDesugarer;
import tools.refinery.language.utils.ProblemUtil;
import tools.refinery.store.dse.propagation.PropagationBuilder;
import tools.refinery.store.model.ModelStoreBuilder;
import tools.refinery.store.query.Constraint;
import tools.refinery.store.query.dnf.InvalidClauseException;
import tools.refinery.store.query.dnf.Query;
import tools.refinery.store.query.dnf.RelationalQuery;
import tools.refinery.store.query.literal.*;
import tools.refinery.store.query.term.NodeVariable;
import tools.refinery.store.query.term.Variable;
import tools.refinery.store.reasoning.ReasoningAdapter;
import tools.refinery.store.reasoning.representation.AnyPartialSymbol;
import tools.refinery.store.reasoning.representation.PartialRelation;
import tools.refinery.store.reasoning.scope.ScopePropagator;
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

	private final Map<PartialRelation, RelationInfo> partialRelationInfoMap = new HashMap<>();

	private final Map<AnyPartialSymbol, Relation> inverseTrace = new HashMap<>();

	private Map<Relation, PartialRelation> relationTrace;

	private final MetamodelBuilder metamodelBuilder = Metamodel.builder();

	private Metamodel metamodel;

	private final Map<Tuple, CardinalityInterval> countSeed = new LinkedHashMap<>();

	private ScopePropagator scopePropagator;

	private ModelSeed modelSeed;

	public Problem getProblem() {
		return problem;
	}

	public int getNodeCount() {
		return nodeTrace.size();
	}

	public MutableObjectIntMap<Node> getNodeTrace() {
		return nodeTrace;
	}

	public Map<Relation, PartialRelation> getRelationTrace() {
		return relationTrace;
	}

	public Relation getInverseTrace(AnyPartialSymbol partialRelation) {
		return inverseTrace.get(partialRelation);
	}

	public Metamodel getMetamodel() {
		return metamodel;
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
		putRelationInfo(builtinSymbols.invalidContainer(),
				new RelationInfo(ContainmentHierarchyTranslator.INVALID_CONTAINER, TruthValue.FALSE,
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
		collectScopes();
		if (scopePropagator != null) {
			if (storeBuilder.tryGetAdapter(PropagationBuilder.class).isEmpty()) {
				throw new TracedException(problem, "Type scopes require a PropagationBuilder");
			}
			storeBuilder.with(scopePropagator);
		}
		modelSeedBuilder.seed(MultiObjectTranslator.COUNT_SYMBOL, builder -> builder
				.reducedValue(CardinalityIntervals.SET)
				.putAll(countSeed));
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
				collectPartialRelation(enumDeclaration, 1, TruthValue.FALSE, TruthValue.FALSE);
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
				throw new TracedException(featureDeclaration, "Unknown feature declaration");
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
		inverseTrace.put(info.partialRelation(), relation);
	}

	private RelationInfo collectPartialRelation(Relation relation, int arity, TruthValue value,
												TruthValue defaultValue) {
		return relationInfoMap.computeIfAbsent(relation, key -> {
			var name = getName(relation);
			var info = new RelationInfo(name, arity, value, defaultValue);
			partialRelationInfoMap.put(info.partialRelation(), info);
			inverseTrace.put(info.partialRelation(), relation);
			return info;
		});
	}

	private String getName(Relation relation) {
		return semanticsUtils.getName(relation).orElseGet(() -> "::" + relationInfoMap.size());
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
		try {
			metamodelBuilder.type(getPartialRelation(enumDeclaration), nodeRelation);
		} catch (RuntimeException e) {
			throw TracedException.addTrace(enumDeclaration, e);
		}
	}

	private void collectClassDeclarationMetamodel(ClassDeclaration classDeclaration) {
		var superTypes = classDeclaration.getSuperTypes();
		var partialSuperTypes = new ArrayList<PartialRelation>(superTypes.size() + 1);
		partialSuperTypes.add(nodeRelation);
		for (var superType : superTypes) {
			partialSuperTypes.add(getPartialRelation(superType));
		}
		try {
			metamodelBuilder.type(getPartialRelation(classDeclaration), classDeclaration.isAbstract(),
					partialSuperTypes);
		} catch (RuntimeException e) {
			throw TracedException.addTrace(classDeclaration, e);
		}
		for (var featureDeclaration : classDeclaration.getFeatureDeclarations()) {
			if (featureDeclaration instanceof ReferenceDeclaration referenceDeclaration) {
				collectReferenceDeclarationMetamodel(classDeclaration, referenceDeclaration);
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
		try {
			metamodelBuilder.reference(relation, source, containment, multiplicity, target, oppositeRelation);
		} catch (RuntimeException e) {
			throw TracedException.addTrace(classDeclaration, e);
		}
	}

	private Multiplicity getMultiplicityConstraint(ReferenceDeclaration referenceDeclaration) {
		if (!ProblemUtil.hasMultiplicityConstraint(referenceDeclaration)) {
			return UnconstrainedMultiplicity.INSTANCE;
		}
		var problemMultiplicity = referenceDeclaration.getMultiplicity();
		CardinalityInterval interval;
		if (problemMultiplicity == null) {
			interval = CardinalityIntervals.LONE;
		} else {
			interval = getCardinalityInterval(problemMultiplicity);
		}
		var constraint = getRelationInfo(referenceDeclaration.getInvalidMultiplicity()).partialRelation();
		return ConstrainedMultiplicity.of(interval, constraint);
	}

	private static CardinalityInterval getCardinalityInterval(
			tools.refinery.language.model.problem.Multiplicity problemMultiplicity) {
		if (problemMultiplicity instanceof ExactMultiplicity exactMultiplicity) {
			return CardinalityIntervals.exactly(exactMultiplicity.getExactValue());
		} else if (problemMultiplicity instanceof RangeMultiplicity rangeMultiplicity) {
			var upperBound = rangeMultiplicity.getUpperBound();
			return CardinalityIntervals.between(rangeMultiplicity.getLowerBound(),
					upperBound < 0 ? UpperCardinalities.UNBOUNDED : UpperCardinalities.atMost(upperBound));
		} else {
			throw new TracedException(problemMultiplicity, "Unknown multiplicity");
		}
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
		var overlay = MutableSeed.of(1, null);
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
		var tuple = getTuple(assertion);
		var value = getTruthValue(assertion.getValue());
		var relation = assertion.getRelation();
		var info = getRelationInfo(relation);
		var partialRelation = info.partialRelation();
		if (partialRelation.arity() != tuple.getSize()) {
			throw new TracedException(assertion, "Expected %d arguments for %s, got %d instead"
					.formatted(partialRelation.arity(), partialRelation, tuple.getSize()));
		}
		if (assertion.isDefault()) {
			info.defaultAssertions().mergeValue(tuple, value);
		} else {
			info.assertions().mergeValue(tuple, value);
		}
	}

	private void mergeValue(Relation relation, Tuple key, TruthValue value) {
		getRelationInfo(relation).assertions().mergeValue(key, value);
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
				throw new TracedException(argument, "Unsupported assertion argument");
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
				collectPredicateDefinitionTraced(predicateDefinition);
			}
		}
	}

	private void collectPredicateDefinitionTraced(PredicateDefinition predicateDefinition) {
		try {
			collectPredicateDefinition(predicateDefinition);
		} catch (InvalidClauseException e) {
			int clauseIndex = e.getClauseIndex();
			var bodies = predicateDefinition.getBodies();
			if (clauseIndex < bodies.size()) {
				throw new TracedException(bodies.get(clauseIndex), e);
			} else {
				throw new TracedException(predicateDefinition, e);
			}
		} catch (RuntimeException e) {
			throw TracedException.addTrace(predicateDefinition, e);
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
			try {
				var localScope = extendScope(parameterMap, body.getImplicitVariables());
				var problemLiterals = body.getLiterals();
				var literals = new ArrayList<>(commonLiterals);
				for (var problemLiteral : problemLiterals) {
					toLiteralsTraced(problemLiteral, localScope, literals);
				}
				builder.clause(literals);
			} catch (RuntimeException e) {
				throw TracedException.addTrace(body, e);
			}
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

	private void toLiteralsTraced(Expr expr, Map<tools.refinery.language.model.problem.Variable, Variable> localScope,
								  List<Literal> literals) {
		try {
			toLiterals(expr, localScope, literals);
		} catch (RuntimeException e) {
			throw TracedException.addTrace(expr, e);
		}
	}

	private void toLiterals(Expr expr, Map<tools.refinery.language.model.problem.Variable, Variable> localScope,
							List<Literal> literals) {
		if (expr instanceof LogicConstant logicConstant) {
			switch (logicConstant.getLogicValue()) {
			case TRUE -> literals.add(BooleanLiteral.TRUE);
			case FALSE -> literals.add(BooleanLiteral.FALSE);
			default -> throw new TracedException(logicConstant, "Unsupported literal");
			}
		} else if (expr instanceof Atom atom) {
			var target = getPartialRelation(atom.getRelation());
			var polarity = atom.isTransitiveClosure() ? CallPolarity.TRANSITIVE : CallPolarity.POSITIVE;
			var argumentList = toArgumentList(atom.getArguments(), localScope, literals);
			literals.add(target.call(polarity, argumentList));
		} else if (expr instanceof NegationExpr negationExpr) {
			var body = negationExpr.getBody();
			if (!(body instanceof Atom atom)) {
				throw new TracedException(body, "Cannot negate literal");
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
				default -> throw new TracedException(
						comparisonExpr, "Unsupported operator");
			};
			literals.add(new EquivalenceLiteral(positive, argumentList.get(0), argumentList.get(1)));
		} else {
			throw new TracedException(expr, "Unsupported literal");
		}
	}

	private List<Variable> toArgumentList(
			List<Expr> expressions, Map<tools.refinery.language.model.problem.Variable, Variable> localScope,
			List<Literal> literals) {
		var argumentList = new ArrayList<Variable>(expressions.size());
		for (var expr : expressions) {
			if (!(expr instanceof VariableOrNodeExpr variableOrNodeExpr)) {
				throw new TracedException(expr, "Unsupported argument");
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
						throw new TracedException(variableOrNode, "Unknown variable: " + problemVariable.getName());
					}
					argumentList.add(variable);
				}
			} else {
				throw new TracedException(variableOrNode, "Unknown argument");
			}
		}
		return argumentList;
	}

	private void collectScopes() {
		for (var statement : problem.getStatements()) {
			if (statement instanceof ScopeDeclaration scopeDeclaration) {
				for (var typeScope : scopeDeclaration.getTypeScopes()) {
					if (typeScope.isIncrement()) {
						collectTypeScopeIncrement(typeScope);
					} else {
						collectTypeScope(typeScope);
					}
				}
			}
		}
	}

	private void collectTypeScopeIncrement(TypeScope typeScope) {
		if (!(typeScope.getTargetType() instanceof ClassDeclaration classDeclaration)) {
			throw new TracedException(typeScope, "Target of incremental type scope must be a class declaration");
		}
		var newNode = classDeclaration.getNewNode();
		if (newNode == null) {
			throw new TracedException(typeScope, "Target of incremental type scope must be concrete class");
		}
		int newNodeId = nodeTrace.get(newNode);
		var type = relationTrace.get(classDeclaration);
		var typeInfo = metamodel.typeHierarchy().getAnalysisResult(type);
		if (!typeInfo.getDirectSubtypes().isEmpty()) {
			throw new TracedException(typeScope, "Target of incremental type scope cannot have any subclasses");
		}
		var interval = getCardinalityInterval(typeScope.getMultiplicity());
		countSeed.compute(Tuple.of(newNodeId), (key, oldValue) ->
				oldValue == null ? interval : oldValue.meet(interval));
	}

	private void collectTypeScope(TypeScope typeScope) {
		var type = relationTrace.get(typeScope.getTargetType());
		if (type == null) {
			throw new TracedException(typeScope, "Unknown target type");
		}
		var interval = getCardinalityInterval(typeScope.getMultiplicity());
		if (scopePropagator == null) {
			scopePropagator = new ScopePropagator();
		}
		scopePropagator.scope(type, interval);
	}

	private record RelationInfo(PartialRelation partialRelation, MutableSeed<TruthValue> assertions,
								MutableSeed<TruthValue> defaultAssertions) {
		public RelationInfo(String name, int arity, TruthValue value, TruthValue defaultValue) {
			this(new PartialRelation(name, arity), value, defaultValue);
		}

		public RelationInfo(PartialRelation partialRelation, TruthValue value, TruthValue defaultValue) {
			this(partialRelation, MutableSeed.of(partialRelation.arity(), value),
					MutableSeed.of(partialRelation.arity(), defaultValue));
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
