/*
 * SPDX-FileCopyrightText: 2021-2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.semantics;

import com.google.inject.Inject;
import org.jetbrains.annotations.Nullable;
import tools.refinery.language.library.BuiltinLibrary;
import tools.refinery.language.model.problem.*;
import tools.refinery.language.scoping.imports.ImportAdapterProvider;
import tools.refinery.language.scoping.imports.ImportCollector;
import tools.refinery.language.semantics.internal.MutableSeed;
import tools.refinery.language.utils.BuiltinSymbols;
import tools.refinery.language.utils.ProblemUtil;
import tools.refinery.logic.Constraint;
import tools.refinery.logic.dnf.*;
import tools.refinery.logic.literal.*;
import tools.refinery.logic.term.NodeVariable;
import tools.refinery.logic.term.Variable;
import tools.refinery.logic.term.cardinalityinterval.CardinalityInterval;
import tools.refinery.logic.term.cardinalityinterval.CardinalityIntervals;
import tools.refinery.logic.term.truthvalue.TruthValue;
import tools.refinery.logic.term.uppercardinality.UpperCardinalities;
import tools.refinery.store.dse.propagation.PropagationBuilder;
import tools.refinery.store.dse.transition.DesignSpaceExplorationBuilder;
import tools.refinery.store.dse.transition.Rule;
import tools.refinery.store.dse.transition.RuleBuilder;
import tools.refinery.store.dse.transition.actions.ActionLiteral;
import tools.refinery.store.dse.transition.actions.ActionLiterals;
import tools.refinery.store.model.ModelStoreBuilder;
import tools.refinery.store.reasoning.ReasoningAdapter;
import tools.refinery.store.reasoning.actions.PartialActionLiterals;
import tools.refinery.store.reasoning.literal.Concreteness;
import tools.refinery.store.reasoning.literal.ModalConstraint;
import tools.refinery.store.reasoning.literal.Modality;
import tools.refinery.store.reasoning.representation.PartialRelation;
import tools.refinery.store.reasoning.scope.ScopePropagator;
import tools.refinery.store.reasoning.seed.ModelSeed;
import tools.refinery.store.reasoning.seed.Seed;
import tools.refinery.store.reasoning.translator.TranslationException;
import tools.refinery.store.reasoning.translator.containment.ContainmentHierarchyTranslator;
import tools.refinery.store.reasoning.translator.metamodel.Metamodel;
import tools.refinery.store.reasoning.translator.metamodel.MetamodelBuilder;
import tools.refinery.store.reasoning.translator.metamodel.MetamodelTranslator;
import tools.refinery.store.reasoning.translator.metamodel.ReferenceInfo;
import tools.refinery.store.reasoning.translator.multiobject.MultiObjectTranslator;
import tools.refinery.store.reasoning.translator.multiplicity.ConstrainedMultiplicity;
import tools.refinery.store.reasoning.translator.multiplicity.Multiplicity;
import tools.refinery.store.reasoning.translator.multiplicity.UnconstrainedMultiplicity;
import tools.refinery.store.reasoning.translator.predicate.PredicateTranslator;
import tools.refinery.store.tuple.Tuple;

import java.util.*;

public class ModelInitializer {
	@Inject
	private SemanticsUtils semanticsUtils;

	@Inject
	private ProblemTraceImpl problemTrace;

	@Inject
	private ImportCollector importCollector;

	@Inject
	private ImportAdapterProvider importAdapterProvider;

	private Problem problem;

	private final Set<Problem> importedProblems = new HashSet<>();

	private BuiltinSymbols builtinSymbols;

	private PartialRelation nodeRelation;

	private final Map<Relation, RelationInfo> relationInfoMap = new LinkedHashMap<>();

	private final Map<PartialRelation, RelationInfo> partialRelationInfoMap = new HashMap<>();

	private final Set<PartialRelation> targetTypes = new HashSet<>();

	private final MetamodelBuilder metamodelBuilder = Metamodel.builder();

	private Metamodel metamodel;

	private final Map<Tuple, CardinalityInterval> countSeed = new LinkedHashMap<>();

	private ScopePropagator scopePropagator;

	private int nodeCount;

	private ModelSeed.Builder modelSeedBuilder;

	private ModelSeed modelSeed;

	private int ruleCount;

	public void readProblem(Problem problem) {
		if (this.problem != null) {
			throw new IllegalArgumentException("Problem was already set");
		}
		this.problem = problem;
		loadImportedProblems();
		importedProblems.add(problem);
		problemTrace.setProblem(problem);
		try {
			builtinSymbols = importAdapterProvider.getBuiltinSymbols(problem);
			var nodeInfo = collectPartialRelation(builtinSymbols.node(), 1, TruthValue.TRUE, TruthValue.TRUE);
			nodeRelation = nodeInfo.partialRelation();
			metamodelBuilder.type(nodeRelation);
			putRelationInfo(builtinSymbols.exists(), new RelationInfo(ReasoningAdapter.EXISTS_SYMBOL, null,
					TruthValue.TRUE));
			putRelationInfo(builtinSymbols.equals(), new RelationInfo(ReasoningAdapter.EQUALS_SYMBOL,
					(TruthValue) null,
					null));
			putRelationInfo(builtinSymbols.contained(),
					new RelationInfo(ContainmentHierarchyTranslator.CONTAINED_SYMBOL, null, TruthValue.UNKNOWN));
			putRelationInfo(builtinSymbols.contains(), new RelationInfo(ContainmentHierarchyTranslator.CONTAINS_SYMBOL,
					null, TruthValue.UNKNOWN));
			putRelationInfo(builtinSymbols.invalidContainer(),
					new RelationInfo(ContainmentHierarchyTranslator.INVALID_CONTAINER, TruthValue.FALSE,
							TruthValue.FALSE));
			collectNodes();
			collectPartialSymbols();
			nodeCount = problemTrace.getNodeTrace().size();
			modelSeedBuilder = ModelSeed.builder(nodeCount);
			collectAssertions();
			collectMetamodel();
			metamodel = metamodelBuilder.build();
			problemTrace.setMetamodel(metamodel);
			fixClassDeclarationAssertions();
			for (var entry : relationInfoMap.entrySet()) {
				if (entry.getKey() instanceof ReferenceDeclaration) {
					continue;
				}
				var info = entry.getValue();
				var partialRelation = info.partialRelation();
				modelSeedBuilder.seed(partialRelation, info.toSeed(nodeCount));
			}
			collectScopes();
			modelSeedBuilder.seed(MultiObjectTranslator.COUNT_SYMBOL, builder -> builder
					.reducedValue(CardinalityIntervals.SET)
					.putAll(countSeed));
			modelSeed = modelSeedBuilder.build();
		} catch (TranslationException e) {
			throw problemTrace.wrapException(e);
		}
	}

	private void loadImportedProblems() {
		var resource = problem.eResource();
		if (resource == null) {
			return;
		}
		var resourceSet = resource.getResourceSet();
		if (resourceSet == null) {
			return;
		}
		var importedUris = importCollector.getAllImports(resource).toUriSet();
		for (var uri : importedUris) {
			if (BuiltinLibrary.BUILTIN_LIBRARY_URI.equals(uri)) {
				// We hard-code the behavior of the builtin library.
				continue;
			}
			var importedResource = resourceSet.getResource(uri, false);
			if (importedResource != null && !importedResource.getContents().isEmpty() &&
					importedResource.getContents().getFirst() instanceof Problem importedProblem) {
				importedProblems.add(importedProblem);
			}
		}
	}

	public void configureStoreBuilder(ModelStoreBuilder storeBuilder) {
		checkProblem();
		try {
			storeBuilder.with(new MultiObjectTranslator());
			storeBuilder.with(new MetamodelTranslator(metamodel));
			if (scopePropagator != null) {
				if (storeBuilder.tryGetAdapter(PropagationBuilder.class).isEmpty()) {
					throw new TracedException(problem, "Type scopes require a PropagationBuilder");
				}
				storeBuilder.with(scopePropagator);
			}
			collectPredicates(storeBuilder);
			collectRules(storeBuilder);
		} catch (TranslationException e) {
			throw problemTrace.wrapException(e);
		}
	}

	private void checkProblem() {
		if (problem == null) {
			throw new IllegalStateException("Problem is not set");
		}
	}

	public ModelSeed createModel(Problem problem, ModelStoreBuilder storeBuilder) {
		readProblem(problem);
		configureStoreBuilder(storeBuilder);
		return getModelSeed();
	}

	public ProblemTrace getProblemTrace() {
		checkProblem();
		return problemTrace;
	}

	public ModelSeed getModelSeed() {
		checkProblem();
		return modelSeed;
	}

	private void collectNodes() {
		for (var importedProblem : importedProblems) {
			for (var statement : importedProblem.getStatements()) {
				collectNodes(statement);
			}
		}
		for (var node : problem.getNodes()) {
			collectNode(node);
		}
	}

	private void collectNodes(Statement statement) {
		if (statement instanceof NodeDeclaration nodeDeclaration) {
			for (var node : nodeDeclaration.getNodes()) {
				collectNode(node);
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

	private void collectNode(Node node) {
		problemTrace.collectNode(node);
	}

	private void collectPartialSymbols() {
		for (var importedProblem : importedProblems) {
			for (var statement : importedProblem.getStatements()) {
				if (statement instanceof ClassDeclaration classDeclaration) {
					collectClassDeclarationSymbols(classDeclaration);
				} else if (statement instanceof EnumDeclaration enumDeclaration) {
					collectPartialRelation(enumDeclaration, 1, TruthValue.FALSE, TruthValue.FALSE);
				} else if (statement instanceof PredicateDefinition predicateDefinition) {
					collectPredicateDefinitionSymbol(predicateDefinition);
				}
			}
		}
	}

	private void collectClassDeclarationSymbols(ClassDeclaration classDeclaration) {
		collectPartialRelation(classDeclaration, 1, null, TruthValue.UNKNOWN);
		for (var referenceDeclaration : classDeclaration.getFeatureDeclarations()) {
			if (referenceDeclaration.getReferenceType() instanceof DatatypeDeclaration) {
				throw new TracedException(referenceDeclaration, "Attributes are not yet supported");
			}
			collectPartialRelation(referenceDeclaration, 2, null, TruthValue.UNKNOWN);
			var invalidMultiplicityConstraint = referenceDeclaration.getInvalidMultiplicity();
			if (invalidMultiplicityConstraint != null) {
				collectPartialRelation(invalidMultiplicityConstraint, 1, TruthValue.FALSE, TruthValue.FALSE);
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
		problemTrace.putRelation(relation, info.partialRelation());
	}

	private RelationInfo collectPartialRelation(Relation relation, int arity, TruthValue value,
												TruthValue defaultValue) {
		return relationInfoMap.computeIfAbsent(relation, key -> {
			var name = getName(relation);
			var info = new RelationInfo(name, arity, value, defaultValue);
			partialRelationInfoMap.put(info.partialRelation(), info);
			problemTrace.putRelation(relation, info.partialRelation());
			return info;
		});
	}

	private String getName(Relation relation) {
		return semanticsUtils.getNameWithoutRootPrefix(relation).orElseGet(() -> "::" + relationInfoMap.size());
	}

	private void collectMetamodel() {
		for (var importedProblem : importedProblems) {
			for (var statement : importedProblem.getStatements()) {
				if (statement instanceof ClassDeclaration classDeclaration) {
					collectClassDeclarationMetamodel(classDeclaration);
				} else if (statement instanceof EnumDeclaration enumDeclaration) {
					collectEnumMetamodel(enumDeclaration);
				}
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
		for (var referenceDeclaration : classDeclaration.getFeatureDeclarations()) {
			collectReferenceDeclarationMetamodel(classDeclaration, referenceDeclaration);
		}
	}

	private void collectReferenceDeclarationMetamodel(ClassDeclaration classDeclaration,
													  ReferenceDeclaration referenceDeclaration) {
		var relation = getPartialRelation(referenceDeclaration);
		var source = getPartialRelation(classDeclaration);
		var target = getPartialRelation(referenceDeclaration.getReferenceType());
		targetTypes.add(target);
		boolean containment = referenceDeclaration.getKind() == ReferenceKind.CONTAINMENT;
		boolean partial = referenceDeclaration.getKind() == ReferenceKind.PARTIAL;
		var opposite = referenceDeclaration.getOpposite();
		PartialRelation oppositeRelation = null;
		if (opposite != null) {
			oppositeRelation = getPartialRelation(opposite);
		}
		var multiplicity = getMultiplicityConstraint(referenceDeclaration);
		try {
			var seed = relationInfoMap.get(referenceDeclaration).toSeed(nodeCount);
			var defaultValue = seed.majorityValue();
			if (defaultValue.must()) {
				defaultValue = TruthValue.FALSE;
			}
			modelSeedBuilder.seed(relation, seed);
			metamodelBuilder.reference(relation, ReferenceInfo.builder()
					.containment(containment)
					.source(source)
					.multiplicity(multiplicity)
					.target(target)
					.opposite(oppositeRelation)
					.defaultValue(defaultValue)
					.partial(partial)
					.build());
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
		switch (problemMultiplicity) {
		case ExactMultiplicity exactMultiplicity -> {
			return CardinalityIntervals.exactly(exactMultiplicity.getExactValue());
		}
		case RangeMultiplicity rangeMultiplicity -> {
			var upperBound = rangeMultiplicity.getUpperBound();
			return CardinalityIntervals.between(rangeMultiplicity.getLowerBound(),
					upperBound < 0 ? UpperCardinalities.UNBOUNDED : UpperCardinalities.atMost(upperBound));
		}
		default -> throw new TracedException(problemMultiplicity, "Unknown multiplicity");
		}
	}

	private void collectAssertions() {
		for (var importedProblem : importedProblems) {
			for (var statement : importedProblem.getStatements()) {
				if (statement instanceof ClassDeclaration classDeclaration) {
					collectClassDeclarationAssertions(classDeclaration);
				} else if (statement instanceof EnumDeclaration enumDeclaration) {
					collectEnumAssertions(enumDeclaration);
				} else if (statement instanceof NodeDeclaration nodeDeclaration) {
					collectNodeDeclarationAssertions(nodeDeclaration);
				} else if (statement instanceof Assertion assertion) {
					collectAssertion(assertion);
				}
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
	}

	private void collectEnumAssertions(EnumDeclaration enumDeclaration) {
		var overlay = MutableSeed.of(1, null);
		for (var literal : enumDeclaration.getLiterals()) {
			collectCardinalityAssertions(literal, TruthValue.TRUE);
			var nodeId = getNodeId(literal);
			overlay.mergeValue(Tuple.of(nodeId), TruthValue.TRUE);
		}
		var info = getRelationInfo(enumDeclaration);
		info.assertions().overwriteValues(overlay);
	}

	private void collectNodeDeclarationAssertions(NodeDeclaration nodeDeclaration) {
		var kind = nodeDeclaration.getKind();
		TruthValue value;
		switch (kind) {
		case ATOM -> value = TruthValue.TRUE;
		case MULTI -> value = TruthValue.UNKNOWN;
		case NODE -> {
			return;
		}
		default -> throw new IllegalArgumentException("Unknown node kind: " + kind);
		}
		for (var node : nodeDeclaration.getNodes()) {
			collectCardinalityAssertions(node, value);
		}
	}

	private void collectCardinalityAssertions(Node node, TruthValue value) {
		var nodeId = getNodeId(node);
		collectCardinalityAssertions(nodeId, value);
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

	private void fixClassDeclarationAssertions() {
		for (var importedProblem : importedProblems) {
			for (var statement : importedProblem.getStatements()) {
				if (statement instanceof ClassDeclaration classDeclaration) {
					fixClassDeclarationAssertions(classDeclaration);
				}
			}
		}
	}

	private void fixClassDeclarationAssertions(ClassDeclaration classDeclaration) {
		var newNode = classDeclaration.getNewNode();
		if (newNode == null) {
			return;
		}
		var newNodeId = getNodeId(newNode);
		var tuple = Tuple.of(newNodeId);
		var typeInfo = metamodel.typeHierarchy().getAnalysisResult(getPartialRelation(classDeclaration));
		for (var subType : typeInfo.getDirectSubtypes()) {
			partialRelationInfoMap.get(subType).assertions().mergeValue(tuple, TruthValue.FALSE);
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
		return problemTrace.getNodeId(node);
	}

	private Tuple getTuple(Assertion assertion) {
		var arguments = assertion.getArguments();
		int arity = arguments.size();
		var nodes = new int[arity];
		for (int i = 0; i < arity; i++) {
			var argument = arguments.get(i);
			if (argument instanceof NodeAssertionArgument nodeArgument) {
				var variableOrNode = nodeArgument.getNode();
				if (variableOrNode instanceof Node node) {
					nodes[i] = getNodeId(node);
				} else {
					throw new TracedException(argument, "Invalid assertion argument: " + variableOrNode);
				}
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

	private void collectPredicates(ModelStoreBuilder storeBuilder) {
		for (var importedProblem : importedProblems) {
			for (var statement : importedProblem.getStatements()) {
				if (statement instanceof PredicateDefinition predicateDefinition) {
					collectPredicateDefinitionTraced(predicateDefinition, storeBuilder);
				}
			}
		}
	}

	private void collectPredicateDefinitionTraced(PredicateDefinition predicateDefinition,
												  ModelStoreBuilder storeBuilder) {
		try {
			collectPredicateDefinition(predicateDefinition, storeBuilder);
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

	private void collectPredicateDefinition(PredicateDefinition predicateDefinition, ModelStoreBuilder storeBuilder) {
		var partialRelation = getPartialRelation(predicateDefinition);
		var query = toQuery(partialRelation.name(), predicateDefinition);
		boolean mutable = targetTypes.contains(partialRelation);
		TruthValue defaultValue;
		if (predicateDefinition.isError()) {
			defaultValue = TruthValue.FALSE;
		} else {
			var seed = modelSeed.getSeed(partialRelation);
			defaultValue = seed.majorityValue() == TruthValue.FALSE ? TruthValue.FALSE : TruthValue.UNKNOWN;
			var cursor = seed.getCursor(defaultValue, problemTrace.getNodeTrace().size());
			// The symbol should be mutable if there is at least one non-default entry in the seed.
			mutable = mutable || cursor.move();
		}
		var translator = new PredicateTranslator(partialRelation, query, mutable, defaultValue);
		storeBuilder.with(translator);
	}

	private RelationalQuery toQuery(String name, PredicateDefinition predicateDefinition) {
		var problemParameters = predicateDefinition.getParameters();
		int arity = problemParameters.size();
		var parameters = new NodeVariable[arity];
		var parameterMap = HashMap.<tools.refinery.language.model.problem.Variable, Variable>newHashMap(arity);
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
			buildConjunction(body, parameterMap, commonLiterals, builder);
		}
		return builder.build();
	}

	private void buildConjunction(
			Conjunction body, HashMap<tools.refinery.language.model.problem.Variable, ? extends Variable> parameterMap,
			List<Literal> commonLiterals, AbstractQueryBuilder<?> builder) {
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

	private Map<tools.refinery.language.model.problem.Variable, ? extends Variable> extendScope(
			Map<tools.refinery.language.model.problem.Variable, ? extends Variable> existing,
			Collection<? extends tools.refinery.language.model.problem.Variable> newVariables) {
		if (newVariables.isEmpty()) {
			return existing;
		}
		int localScopeSize = existing.size() + newVariables.size();
		var localScope = HashMap.<tools.refinery.language.model.problem.Variable, Variable>newHashMap(localScopeSize);
		localScope.putAll(existing);
		for (var newVariable : newVariables) {
			localScope.put(newVariable, Variable.of(newVariable.getName()));
		}
		return localScope;
	}

	private void toLiteralsTraced(
			Expr expr, Map<tools.refinery.language.model.problem.Variable, ? extends Variable> localScope,
			List<Literal> literals) {
		try {
			toLiterals(expr, localScope, literals);
		} catch (RuntimeException e) {
			throw TracedException.addTrace(expr, e);
		}
	}

	private void toLiterals(
			Expr expr, Map<tools.refinery.language.model.problem.Variable, ? extends Variable> localScope,
			List<Literal> literals) {
		var extractedOuter = extractModalExpr(expr);
		var outerModality = extractedOuter.modality();
		switch (extractedOuter.body()) {
		case LogicConstant logicConstant -> {
			switch (logicConstant.getLogicValue()) {
			case TRUE -> literals.add(BooleanLiteral.TRUE);
			case FALSE -> literals.add(BooleanLiteral.FALSE);
			default -> throw new TracedException(logicConstant, "Unsupported literal");
			}
		}
		case Atom atom -> {
			var target = getPartialRelation(atom.getRelation());
			var constraint = atom.isTransitiveClosure() ? getTransitiveWrapper(target) : target;
			var argumentList = toArgumentList(atom.getArguments(), localScope, literals);
			literals.add(extractedOuter.modality.wrapConstraint(constraint).call(CallPolarity.POSITIVE, argumentList));
		}
		case NegationExpr negationExpr -> {
			var body = negationExpr.getBody();
			var extractedInner = extractModalExpr(body);
			if (!(extractedInner.body() instanceof Atom atom)) {
				throw new TracedException(extractedInner.body(), "Cannot negate literal");
			}
			var target = getPartialRelation(atom.getRelation());
			Constraint constraint = atom.isTransitiveClosure() ? getTransitiveWrapper(target) : target;
			var negatedScope = extendScope(localScope, negationExpr.getImplicitVariables());
			List<Variable> argumentList = toArgumentList(atom.getArguments(), negatedScope, literals);
			var innerModality = extractedInner.modality().merge(outerModality.negate());
			literals.add(createNegationLiteral(innerModality, constraint, argumentList, localScope));
		}
		case ComparisonExpr comparisonExpr -> {
			var argumentList = toArgumentList(List.of(comparisonExpr.getLeft(), comparisonExpr.getRight()),
					localScope, literals);
			boolean positive = switch (comparisonExpr.getOp()) {
				case NODE_EQ -> true;
				case NODE_NOT_EQ -> false;
				default -> throw new TracedException(
						comparisonExpr, "Unsupported operator");
			};
			literals.add(createEquivalenceLiteral(outerModality, positive, argumentList.get(0), argumentList.get(1),
					localScope));
		}
		default -> throw new TracedException(extractedOuter.body(), "Unsupported literal");
		}
	}

	private Constraint getTransitiveWrapper(Constraint target) {
		return Query.of(target.name() + "#transitive", (builder, p1, p2) -> builder.clause(
				target.callTransitive(p1, p2)
		)).getDnf();
	}

	private static Literal createNegationLiteral(
			ConcreteModality innerModality, Constraint constraint, List<Variable> argumentList,
			Map<tools.refinery.language.model.problem.Variable, ? extends Variable> localScope) {
		if (innerModality.isSet()) {
			boolean needsQuantification = false;
			var filteredArguments = new LinkedHashSet<Variable>();
			for (var argument : argumentList) {
				if (localScope.containsValue(argument)) {
					filteredArguments.add(argument);
				} else {
					needsQuantification = true;
				}
			}
			// If there are any quantified arguments, set a helper pattern to be lifted so that the appropriate
			// {@code EXISTS} call are added by the {@code DnfLifter}.
			if (needsQuantification) {
				var filteredArgumentList = List.copyOf(filteredArguments);
				var quantifiedConstraint = Dnf.builder(constraint.name() + "#quantified")
						.parameters(filteredArgumentList)
						.clause(
								constraint.call(CallPolarity.POSITIVE, argumentList)
						)
						.build();
				return innerModality.wrapConstraint(quantifiedConstraint)
						.call(CallPolarity.NEGATIVE, filteredArgumentList);
			}
		}
		return innerModality.wrapConstraint(constraint).call(CallPolarity.NEGATIVE, argumentList);
	}

	private Literal createEquivalenceLiteral(
			ConcreteModality outerModality, boolean positive, Variable left, Variable right,
			Map<tools.refinery.language.model.problem.Variable, ? extends Variable> localScope) {
		if (!outerModality.isSet()) {
			return new EquivalenceLiteral(positive, left, right);
		}
		if (positive) {
			return outerModality.wrapConstraint(ReasoningAdapter.EQUALS_SYMBOL).call(left, right);
		}
		// Interpret {@code x != y} as {@code !equals(x, y)} at all times, even in modal operators.
		return createNegationLiteral(outerModality.negate(), ReasoningAdapter.EQUALS_SYMBOL, List.of(left, right),
				localScope);
	}

	private record ConcreteModality(@Nullable Concreteness concreteness, @Nullable Modality modality) {
		public static final ConcreteModality NULL = new ConcreteModality((Concreteness) null, null);

		public ConcreteModality(tools.refinery.language.model.problem.Concreteness concreteness,
								tools.refinery.language.model.problem.Modality modality) {
			this(
					switch (concreteness) {
						case PARTIAL -> Concreteness.PARTIAL;
						case CANDIDATE -> Concreteness.CANDIDATE;
					},
					switch (modality) {
						case MUST -> Modality.MUST;
						case MAY -> Modality.MAY;
						case NONE -> throw new IllegalArgumentException("Invalid modality");
					}
			);
		}

		public ConcreteModality negate() {
			var negatedModality = modality == null ? null : modality.negate();
			return new ConcreteModality(concreteness, negatedModality);
		}

		public ConcreteModality merge(ConcreteModality outer) {
			var mergedConcreteness = concreteness == null ? outer.concreteness() : concreteness;
			var mergedModality = modality == null ? outer.modality() : modality;
			return new ConcreteModality(mergedConcreteness, mergedModality);
		}

		public Constraint wrapConstraint(Constraint inner) {
			if (isSet()) {
				return new ModalConstraint(modality, concreteness, inner);
			}
			return inner;
		}

		public boolean isSet() {
			return concreteness != null || modality != null;
		}
	}

	private record ExtractedModalExpr(ConcreteModality modality, Expr body) {
	}

	private ExtractedModalExpr extractModalExpr(Expr expr) {
		if (expr instanceof ModalExpr modalExpr) {
			return new ExtractedModalExpr(new ConcreteModality(modalExpr.getConcreteness(), modalExpr.getModality()),
					modalExpr.getBody());
		}
		return new ExtractedModalExpr(ConcreteModality.NULL, expr);
	}

	private List<Variable> toArgumentList(
			List<Expr> expressions, Map<tools.refinery.language.model.problem.Variable, ? extends Variable> localScope,
			List<Literal> literals) {
		var argumentList = new ArrayList<Variable>(expressions.size());
		for (var expr : expressions) {
			if (!(expr instanceof VariableOrNodeExpr variableOrNodeExpr)) {
				throw new TracedException(expr, "Unsupported argument");
			}
			var variableOrNode = variableOrNodeExpr.getVariableOrNode();
			switch (variableOrNode) {
			case Node node -> {
				int nodeId = getNodeId(node);
				var tempVariable = Variable.of(semanticsUtils.getNameWithoutRootPrefix(node).orElse("_" + nodeId));
				literals.add(new ConstantLiteral(tempVariable, nodeId));
				argumentList.add(tempVariable);
			}
			case tools.refinery.language.model.problem.Variable problemVariable -> {
				if (variableOrNodeExpr.getSingletonVariable() == problemVariable) {
					argumentList.add(Variable.of(problemVariable.getName()));
				} else {
					var variable = localScope.get(problemVariable);
					if (variable == null) {
						throw new TracedException(variableOrNode, "Unknown variable: " + problemVariable.getName());
					}
					argumentList.add(variable);
				}
			}
			default -> throw new TracedException(variableOrNode, "Unknown argument");
			}
		}
		return argumentList;
	}

	private void collectScopes() {
		for (var importedProblem : importedProblems) {
			for (var statement : importedProblem.getStatements()) {
				collectScopes(statement);
			}
		}
	}

	private void collectScopes(Statement statement) {
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

	private void collectTypeScopeIncrement(TypeScope typeScope) {
		if (!(typeScope.getTargetType() instanceof ClassDeclaration classDeclaration)) {
			throw new TracedException(typeScope, "Target of incremental type scope must be a class declaration");
		}
		var newNode = classDeclaration.getNewNode();
		if (newNode == null) {
			throw new TracedException(typeScope, "Target of incremental type scope must be concrete class");
		}
		int newNodeId = getNodeId(newNode);
		var type = problemTrace.getPartialRelation(classDeclaration);
		var typeInfo = metamodel.typeHierarchy().getAnalysisResult(type);
		if (!typeInfo.getDirectSubtypes().isEmpty()) {
			throw new TracedException(typeScope, "Target of incremental type scope cannot have any subclasses");
		}
		var interval = getCardinalityInterval(typeScope.getMultiplicity());
		countSeed.compute(Tuple.of(newNodeId), (key, oldValue) ->
				oldValue == null ? interval : oldValue.meet(interval));
	}

	private void collectTypeScope(TypeScope typeScope) {
		var type = problemTrace.getPartialRelation(typeScope.getTargetType());
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

	private void collectRules(ModelStoreBuilder storeBuilder) {
		for (var importedProblem : importedProblems) {
			for (var statement : importedProblem.getStatements()) {
				if (statement instanceof RuleDefinition ruleDefinition) {
					collectRule(ruleDefinition, storeBuilder);
				}
			}
		}
	}

	private void collectRule(RuleDefinition ruleDefinition, ModelStoreBuilder storeBuilder) {
		var name = semanticsUtils.getNameWithoutRootPrefix(ruleDefinition)
				.orElseGet(() -> "::rule" + ruleCount);
		ruleCount++;
		var rule = toRule(name, ruleDefinition);
		switch (ruleDefinition.getKind()) {
		case DECISION -> storeBuilder.tryGetAdapter(DesignSpaceExplorationBuilder.class)
				.ifPresent(dseBuilder -> dseBuilder.transformation(rule));
		case PROPAGATION -> storeBuilder.tryGetAdapter(PropagationBuilder.class)
				.ifPresent(propagationBuilder -> propagationBuilder.rule(rule));
		case REFINEMENT -> {
			// Rules not marked for decision or propagation are not invoked automatically.
		}
		}
	}

	private Rule toRule(String name, RuleDefinition ruleDefinition) {
		var problemParameters = ruleDefinition.getParameters();
		int arity = problemParameters.size();
		var parameters = new NodeVariable[arity];
		var parameterMap = HashMap.<tools.refinery.language.model.problem.Variable, NodeVariable>newHashMap(arity);
		var commonLiterals = new ArrayList<Literal>();
		var parametersToFocus = new ArrayList<tools.refinery.language.model.problem.Variable>();
		for (int i = 0; i < arity; i++) {
			var problemParameter = problemParameters.get(i);
			var parameter = Variable.of(problemParameter.getName());
			parameters[i] = parameter;
			parameterMap.put(problemParameter, parameter);
			var parameterType = problemParameter.getParameterType();
			if (parameterType != null) {
				var partialType = getPartialRelation(parameterType);
				var modality = new ConcreteModality(problemParameter.getConcreteness(),
						problemParameter.getModality());
				commonLiterals.add(modality.wrapConstraint(partialType).call(parameter));
			}
			if (ruleDefinition.getKind() == RuleKind.DECISION &&
					problemParameter.getBinding() == ParameterBinding.SINGLE) {
				commonLiterals.add(MultiObjectTranslator.MULTI_VIEW.call(CallPolarity.NEGATIVE, parameter));
			}
			if (problemParameter.getBinding() == ParameterBinding.FOCUS) {
				parametersToFocus.add(problemParameter);
			}
		}
		var builder = Rule.builder(name).parameters(parameters);
		for (var precondition : ruleDefinition.getPreconditions()) {
			buildConjunction(precondition, parameterMap, commonLiterals, builder);
		}
		for (var consequent : ruleDefinition.getConsequents()) {
			buildConsequent(consequent, parameterMap, parametersToFocus, builder);
		}
		return builder.build();
	}

	private void buildConsequent(
			Consequent body, HashMap<tools.refinery.language.model.problem.Variable, NodeVariable> parameterMap,
			Collection<tools.refinery.language.model.problem.Variable> parametersToFocus, RuleBuilder builder) {
		try {
			var actionLiterals = new ArrayList<ActionLiteral>();
			HashMap<tools.refinery.language.model.problem.Variable, NodeVariable> localScope;
			if (parametersToFocus.isEmpty()) {
				localScope = parameterMap;
			} else {
				localScope = new LinkedHashMap<>(parameterMap);
				for (var parameterToFocus : parametersToFocus) {
					var originalParameter = parameterMap.get(parameterToFocus);
					var focusedParameter = Variable.of(originalParameter.getName() + "#focused");
					localScope.put(parameterToFocus, focusedParameter);
					actionLiterals.add(PartialActionLiterals.focus(originalParameter, focusedParameter));
				}
			}
			for (var action : body.getActions()) {
				toActionLiterals(action, localScope, actionLiterals);
			}
			builder.action(actionLiterals);
		} catch (RuntimeException e) {
			throw TracedException.addTrace(body, e);
		}
	}

	private void toActionLiterals(
			Action action, HashMap<tools.refinery.language.model.problem.Variable, NodeVariable> localScope,
			List<ActionLiteral> actionLiterals) {
		if (!(action instanceof AssertionAction assertionAction)) {
			throw new TracedException(action, "Unknown action");
		}
		var partialRelation = getPartialRelation(assertionAction.getRelation());
		var truthValue = getTruthValue(assertionAction.getValue());
		var problemArguments = assertionAction.getArguments();
		var arguments = new NodeVariable[problemArguments.size()];
		for (int i = 0; i < arguments.length; i++) {
			var problemArgument = problemArguments.get(i);
			if (!(problemArgument instanceof NodeAssertionArgument nodeAssertionArgument)) {
				throw new TracedException(problemArgument, "Invalid argument");
			}
			var variableOrNode = nodeAssertionArgument.getNode();
			switch (variableOrNode) {
			case tools.refinery.language.model.problem.Variable problemVariable ->
					arguments[i] = localScope.get(problemVariable);
			case Node node -> {
				int nodeId = getNodeId(node);
				var tempVariable = Variable.of(semanticsUtils.getNameWithoutRootPrefix(node).orElse("_" + nodeId));
				actionLiterals.add(ActionLiterals.constant(tempVariable, nodeId));
				arguments[i] = tempVariable;
			}
			default -> throw new TracedException(problemArgument, "Invalid argument");
			}
		}
		actionLiterals.add(PartialActionLiterals.merge(partialRelation, truthValue, arguments));
	}
}
