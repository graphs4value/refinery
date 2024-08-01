/*
 * SPDX-FileCopyrightText: 2021-2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.semantics;

import com.google.inject.Inject;
import tools.refinery.language.library.BuiltinLibrary;
import tools.refinery.language.model.problem.*;
import tools.refinery.language.scoping.imports.ImportAdapterProvider;
import tools.refinery.language.scoping.imports.ImportCollector;
import tools.refinery.language.semantics.internal.MutableSeed;
import tools.refinery.language.semantics.internal.query.QueryCompiler;
import tools.refinery.language.semantics.internal.query.RuleCompiler;
import tools.refinery.language.utils.BuiltinSymbols;
import tools.refinery.language.utils.ProblemUtil;
import tools.refinery.language.validation.ActionTargetCollector;
import tools.refinery.logic.dnf.InvalidClauseException;
import tools.refinery.logic.term.cardinalityinterval.CardinalityInterval;
import tools.refinery.logic.term.cardinalityinterval.CardinalityIntervals;
import tools.refinery.logic.term.truthvalue.TruthValue;
import tools.refinery.logic.term.uppercardinality.UpperCardinalities;
import tools.refinery.store.dse.propagation.PropagationBuilder;
import tools.refinery.store.dse.transition.DesignSpaceExplorationBuilder;
import tools.refinery.store.model.ModelStoreBuilder;
import tools.refinery.store.reasoning.ReasoningAdapter;
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
import tools.refinery.store.reasoning.translator.predicate.BasePredicateTranslator;
import tools.refinery.store.reasoning.translator.predicate.PredicateTranslator;
import tools.refinery.store.statecoding.StateCoderBuilder;
import tools.refinery.store.tuple.Tuple;
import tools.refinery.store.tuple.Tuple1;

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

	@Inject
	private ActionTargetCollector actionTargetCollector;

	@Inject
	private QueryCompiler queryCompiler;

	@Inject
	private RuleCompiler ruleCompiler;

	private Problem problem;

	private final Set<Problem> importedProblems = new HashSet<>();

	private BuiltinSymbols builtinSymbols;

	private final List<Tuple1> individuals = new ArrayList<>();

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
		queryCompiler.setProblemTrace(problemTrace);
		ruleCompiler.setQueryCompiler(queryCompiler);
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
			putRelationInfo(builtinSymbols.container(),
					new RelationInfo(ContainmentHierarchyTranslator.CONTAINER_SYMBOL, null, TruthValue.UNKNOWN));
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
		configureStoreBuilder(storeBuilder, false);
	}

	public void configureStoreBuilder(ModelStoreBuilder storeBuilder, boolean keepNonExistingObjects) {
		checkProblem();
		try {
			storeBuilder.with(new MultiObjectTranslator(keepNonExistingObjects));
			storeBuilder.with(new MetamodelTranslator(metamodel));
			if (scopePropagator != null) {
				if (storeBuilder.tryGetAdapter(PropagationBuilder.class).isEmpty()) {
					throw new TracedException(problem, "Type scopes require a PropagationBuilder");
				}
				storeBuilder.with(scopePropagator);
			}
			collectPredicates(storeBuilder);
			collectRules(storeBuilder);
			storeBuilder.tryGetAdapter(StateCoderBuilder.class)
					.ifPresent(stateCoderBuilder -> stateCoderBuilder.individuals(individuals));
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
			collectNode(node, false);
		}
	}

	private void collectNodes(Statement statement) {
		if (statement instanceof NodeDeclaration nodeDeclaration) {
			boolean individual = nodeDeclaration.getKind() == NodeKind.ATOM;
			for (var node : nodeDeclaration.getNodes()) {
				collectNode(node, individual);
			}
		} else if (statement instanceof ClassDeclaration classDeclaration) {
			var newNode = classDeclaration.getNewNode();
			if (newNode != null) {
				collectNode(newNode, false);
			}
		} else if (statement instanceof EnumDeclaration enumDeclaration) {
			for (var literal : enumDeclaration.getLiterals()) {
				collectNode(literal, true);
			}
		}
	}

	private void collectNode(Node node, boolean individual) {
		int nodeId = problemTrace.collectNode(node);
		if (individual) {
			individuals.add(Tuple.of(nodeId));
		}
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
		if (predicateDefinition.getKind() == PredicateKind.ERROR) {
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
		var value = SemanticsUtils.getTruthValue(assertion.getValue());
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
		if (ProblemUtil.isBasePredicate(predicateDefinition)) {
			collectBasePredicateDefinition(predicateDefinition, storeBuilder);
		} else {
			collectComputedPredicateDefinition(predicateDefinition, storeBuilder);
		}
	}

	private void collectComputedPredicateDefinition(PredicateDefinition predicateDefinition,
													ModelStoreBuilder storeBuilder) {
		var partialRelation = getPartialRelation(predicateDefinition);
		var query = queryCompiler.toQuery(partialRelation.name(), predicateDefinition);
		List<PartialRelation> parameterTypes = null;
		boolean mutable;
		TruthValue defaultValue;
		if (predicateDefinition.getKind() == PredicateKind.SHADOW) {
			mutable = false;
			defaultValue = TruthValue.UNKNOWN;
		} else {
			mutable = targetTypes.contains(partialRelation) || isActionTarget(predicateDefinition);
			if (predicateDefinition.getKind() == PredicateKind.ERROR) {
				defaultValue = TruthValue.FALSE;
			} else {
				var seed = modelSeed.getSeed(partialRelation);
				defaultValue = seed.majorityValue() == TruthValue.FALSE ? TruthValue.FALSE : TruthValue.UNKNOWN;
				var cursor = seed.getCursor(defaultValue, problemTrace.getNodeTrace().size());
				// The symbol should be mutable if there is at least one non-default entry in the seed.
				mutable = mutable || cursor.move();
			}
			parameterTypes = getParameterTypes(predicateDefinition, null);
		}
		var translator = new PredicateTranslator(partialRelation, query, parameterTypes, mutable, defaultValue);
		storeBuilder.with(translator);
	}

	private boolean isActionTarget(PredicateDefinition predicateDefinition) {
		for (var importedProblem : importedProblems) {
			if (actionTargetCollector.isActionTarget(importedProblem, predicateDefinition)) {
				return true;
			}
		}
		return false;
	}

	private List<PartialRelation> getParameterTypes(ParametricDefinition parametricDefinition,
													PartialRelation defaultType) {
		var parameters = parametricDefinition.getParameters();
		var parameterTypes = new ArrayList<PartialRelation>(parameters.size());
		for (var parameter : parameters) {
			var relation = parameter.getParameterType();
			parameterTypes.add(relation == null ? defaultType : getPartialRelation(relation));
		}
		return Collections.unmodifiableList(parameterTypes);
	}

	private void collectBasePredicateDefinition(PredicateDefinition predicateDefinition,
												ModelStoreBuilder storeBuilder) {
		var partialRelation = getPartialRelation(predicateDefinition);
		var parameterTypes = getParameterTypes(predicateDefinition, nodeRelation);
		var seed = modelSeed.getSeed(partialRelation);
		var defaultValue = seed.majorityValue() == TruthValue.FALSE ? TruthValue.FALSE : TruthValue.UNKNOWN;
		boolean partial = predicateDefinition.getKind() == PredicateKind.PARTIAL;
		var translator = new BasePredicateTranslator(partialRelation, parameterTypes, defaultValue, partial);
		storeBuilder.with(translator);
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
		try {
			var name = semanticsUtils.getNameWithoutRootPrefix(ruleDefinition)
					.orElseGet(() -> "::rule" + ruleCount);
			ruleCount++;
			switch (ruleDefinition.getKind()) {
			case DECISION -> {
				var rule = ruleCompiler.toDecisionRule(name, ruleDefinition);
				problemTrace.putRuleDefinition(ruleDefinition, rule);
				storeBuilder.tryGetAdapter(DesignSpaceExplorationBuilder.class)
						.ifPresent(dseBuilder -> dseBuilder.transformation(rule));
			}
			case PROPAGATION -> {
				var rules = ruleCompiler.toPropagationRules(name, ruleDefinition);
				problemTrace.putPropagationRuleDefinition(ruleDefinition, rules);
				storeBuilder.tryGetAdapter(PropagationBuilder.class)
						.ifPresent(propagationBuilder -> propagationBuilder.rules(rules));
			}
			case REFINEMENT -> {
				// Rules not marked for decision or propagation are not invoked automatically.
				var rule = ruleCompiler.toRule(name, ruleDefinition);
				problemTrace.putRuleDefinition(ruleDefinition, rule);
			}
			}
		} catch (InvalidClauseException e) {
			int clauseIndex = e.getClauseIndex();
			var bodies = ruleDefinition.getPreconditions();
			if (clauseIndex < bodies.size()) {
				throw TracedException.addTrace(bodies.get(clauseIndex), e);
			} else {
				throw TracedException.addTrace(ruleDefinition, e);
			}
		} catch (RuntimeException e) {
			throw TracedException.addTrace(ruleDefinition, e);
		}
	}
}
