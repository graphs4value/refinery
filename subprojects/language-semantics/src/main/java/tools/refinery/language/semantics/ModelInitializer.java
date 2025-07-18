/*
 * SPDX-FileCopyrightText: 2021-2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.semantics;

import com.google.inject.Inject;
import org.eclipse.xtext.naming.IQualifiedNameProvider;
import tools.refinery.language.expressions.ExprToTerm;
import tools.refinery.language.library.BuiltinLibrary;
import tools.refinery.language.model.problem.*;
import tools.refinery.language.scoping.imports.ImportAdapterProvider;
import tools.refinery.language.scoping.imports.ImportCollector;
import tools.refinery.language.semantics.internal.MutableRelationCollector;
import tools.refinery.language.semantics.internal.MutableSeed;
import tools.refinery.language.semantics.internal.query.QueryCompiler;
import tools.refinery.language.semantics.internal.query.RuleCompiler;
import tools.refinery.language.typesystem.DataExprType;
import tools.refinery.language.typesystem.SignatureProvider;
import tools.refinery.language.utils.BuiltinAnnotationContext;
import tools.refinery.language.utils.BuiltinSymbols;
import tools.refinery.language.utils.ProblemUtil;
import tools.refinery.logic.AbstractDomain;
import tools.refinery.logic.AbstractValue;
import tools.refinery.logic.dnf.InvalidClauseException;
import tools.refinery.logic.term.ConstantTerm;
import tools.refinery.logic.term.cardinalityinterval.CardinalityInterval;
import tools.refinery.logic.term.cardinalityinterval.CardinalityIntervals;
import tools.refinery.logic.term.truthvalue.TruthValue;
import tools.refinery.logic.term.uppercardinality.UpperCardinalities;
import tools.refinery.store.dse.propagation.PropagationBuilder;
import tools.refinery.store.dse.transition.DesignSpaceExplorationBuilder;
import tools.refinery.store.dse.transition.Rule;
import tools.refinery.store.model.ModelStoreBuilder;
import tools.refinery.store.reasoning.ReasoningAdapter;
import tools.refinery.store.reasoning.literal.ConcretenessSpecification;
import tools.refinery.store.reasoning.representation.PartialFunction;
import tools.refinery.store.reasoning.representation.PartialRelation;
import tools.refinery.store.reasoning.scope.ScopePropagator;
import tools.refinery.store.reasoning.seed.ModelSeed;
import tools.refinery.store.reasoning.seed.Seed;
import tools.refinery.store.reasoning.translator.ConcretizationSettings;
import tools.refinery.store.reasoning.translator.TranslationException;
import tools.refinery.store.reasoning.translator.attribute.AttributeInfo;
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
import tools.refinery.store.reasoning.translator.predicate.ShadowPredicateTranslator;
import tools.refinery.store.statecoding.StateCoderBuilder;
import tools.refinery.store.tuple.Tuple;
import tools.refinery.store.tuple.Tuple1;

import java.util.*;
import java.util.stream.Collectors;

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
	private MutableRelationCollector mutableRelationCollector;

	@Inject
	private BuiltinAnnotationContext builtinAnnotationContext;

	@Inject
	private IQualifiedNameProvider qualifiedNameProvider;

	@Inject
	private SignatureProvider signatureProvider;

	@Inject
	private QueryCompiler queryCompiler;

	@Inject
	private RuleCompiler ruleCompiler;

	@Inject
	private ExprToTerm exprToTerm;

	private boolean keepNonExistingObjects;

	private boolean keepShadowPredicates = true;

	private Problem problem;

	private final Set<Problem> importedProblems = new HashSet<>();

	private BuiltinSymbols builtinSymbols;

	private final List<Tuple1> individuals = new ArrayList<>();

	private PartialRelation nodeRelation;

	private final Map<Relation, RelationInfo> relationInfoMap = new LinkedHashMap<>();

	private final Map<PartialRelation, RelationInfo> partialRelationInfoMap = new HashMap<>();

	private final Map<Relation, FunctionInfo<?, ?>> functionInfoMap = new LinkedHashMap<>();

	private final Map<PartialFunction<?, ?>, FunctionInfo<?, ?>> partialFunctionInfoMap = new LinkedHashMap<>();

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
		mutableRelationCollector.collectMutableRelations(importedProblems);
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
			if (!keepShadowPredicates) {
				problemTrace.removeShadowRelations();
			}
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
			if (referenceDeclaration.getReferenceType() instanceof DatatypeDeclaration datatypeDeclaration) {
				collectAttribute(datatypeDeclaration, referenceDeclaration);
				continue;
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
		var computedPredicate = predicateDefinition.getComputedValue();
		if (computedPredicate != null) {
			collectPartialRelation(computedPredicate, arity, null, TruthValue.UNKNOWN);
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

	private void collectAttribute(DatatypeDeclaration datatypeDeclaration, ReferenceDeclaration referenceDeclaration) {
		var type = signatureProvider.getDataType(datatypeDeclaration);
		if (!(type instanceof DataExprType dataExprType)) {
			throw new TracedException(referenceDeclaration, "Invalid type '%s' for attribute '%s'.".formatted(
					type, referenceDeclaration.getName()));
		}
		var abstractDomain = importAdapterProvider.getTermInterpreter(referenceDeclaration)
				.getDomain(dataExprType)
				.orElseThrow(() -> new TracedException(referenceDeclaration,
						"No abstract domain for datatype '%s'.".formatted(datatypeDeclaration.getName())));
		var domain = (AbstractDomain<?, ?>) abstractDomain;
		this.createFunctionInfo(domain, referenceDeclaration);
	}

	private <A extends AbstractValue<A, C>, C> void createFunctionInfo(AbstractDomain<A, C> domain,
																	   ReferenceDeclaration referenceDeclaration) {
		var partialFunction = new PartialFunction<>(referenceDeclaration.getName(), 1, domain);
		var info = new FunctionInfo<>(partialFunction, domain);
		problemTrace.putRelation(referenceDeclaration, partialFunction);
		partialFunctionInfoMap.put(partialFunction, info);
		functionInfoMap.put(referenceDeclaration, info);
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
					builtinAnnotationContext.isClassDeclarationDecide(classDeclaration), partialSuperTypes);
		} catch (RuntimeException e) {
			throw TracedException.addTrace(classDeclaration, e);
		}
		for (var referenceDeclaration : classDeclaration.getFeatureDeclarations()) {
			collectReferenceDeclarationMetamodel(classDeclaration, referenceDeclaration);
		}
	}

	private void collectReferenceDeclarationMetamodel(ClassDeclaration classDeclaration,
													  ReferenceDeclaration referenceDeclaration) {
		var source = getPartialRelation(classDeclaration);
		try {
			if (referenceDeclaration.getReferenceType() instanceof DatatypeDeclaration) {
				var partialFunction = problemTrace.getPartialFunction(referenceDeclaration);
				collectAttributeDeclarationMetamodel((PartialFunction<?, ?>) partialFunction, source,
						referenceDeclaration);
				return;
			}
			var relation = getPartialRelation(referenceDeclaration);
			var target = getPartialRelation(referenceDeclaration.getReferenceType());
			boolean containment = referenceDeclaration.getKind() == ReferenceKind.CONTAINMENT;
			var concretizationSettings = getConcretizationSettings(referenceDeclaration);
			var opposite = referenceDeclaration.getOpposite();
			PartialRelation oppositeRelation = null;
			if (opposite != null) {
				oppositeRelation = getPartialRelation(opposite);
			}
			var multiplicity = getMultiplicityConstraint(referenceDeclaration);
			Set<PartialRelation> supersets = referenceDeclaration.getSuperSets().stream()
					.map(this::getPartialRelation)
					.collect(Collectors.toCollection(LinkedHashSet::new));
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
					.concretizationSettings(concretizationSettings)
					.supersets(supersets)
					.build());
		} catch (RuntimeException e) {
			throw TracedException.addTrace(classDeclaration, e);
		}
	}

	private <A extends AbstractValue<A, C>, C> void collectAttributeDeclarationMetamodel(
			PartialFunction<A, C> partialFunction, PartialRelation source, ReferenceDeclaration referenceDeclaration) {
		// The type of the FunctionInfo always matches the type of the PartialFunction,
		// because we have created them at the same time.
		@SuppressWarnings("unchecked")
		var functionInfo = (FunctionInfo<A, C>) functionInfoMap.get(referenceDeclaration);
		var seed = functionInfo.toSeed();
		modelSeedBuilder.seed(partialFunction, seed);
		metamodelBuilder.attribute(partialFunction, new AttributeInfo(source, seed.majorityValue()));
	}

	private ConcretizationSettings getConcretizationSettings(Relation relation) {
		var problemSettings = builtinAnnotationContext.getConcretizationSettings(relation);
		return new ConcretizationSettings(problemSettings.concretize(), problemSettings.decide());
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
		return new ConstrainedMultiplicity(interval, constraint);
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
		var relation = assertion.getRelation();
		// attribute value declaration
		if (relation instanceof ReferenceDeclaration referenceDeclaration &&
				referenceDeclaration.getReferenceType() instanceof DatatypeDeclaration) {
			var partialFunction = problemTrace.getPartialFunction(relation);
			collectAttributeAssertion(assertion, (PartialFunction<?, ?>) partialFunction);
			return;
		}
		var tuple = getTuple(assertion);
		var value = SemanticsUtils.getTruthValue(assertion.getValue());
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

	private <A extends AbstractValue<A, C>, C> void collectAttributeAssertion(Assertion assertion,
																			  PartialFunction<A, C> partialFunction) {
		var value = assertion.getValue();
		var tuple = getTuple(assertion);
		var term = exprToTerm.toTerm(value);
		if (term.isEmpty()) {
			throw new TracedException(value, "Invalid assertion value expression");
		}
		var simplifiedTerm = term.get().asType(partialFunction.abstractDomain().abstractType()).reduce();
		if (!(simplifiedTerm instanceof ConstantTerm<A> constantTerm)) {
			throw new TracedException(value, "Assertion value must be constant");
		}
		var abstractValue = constantTerm.getValue();
		// We know the only possible type of the PartialFunction.
		@SuppressWarnings("unchecked")
		var info = (FunctionInfo<A, C>) partialFunctionInfoMap.get(partialFunction);
		if (partialFunction.arity() != tuple.getSize()) {
			throw new TracedException(assertion, "Expected %d arguments for %s, got %d instead"
					.formatted(partialFunction.arity(), partialFunction, tuple.getSize()));
		}
		if (assertion.isDefault()) {
			info.defaultAssertions().mergeValue(tuple, abstractValue);
		} else {
			info.assertions().mergeValue(tuple, abstractValue);
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
		for (var superType : typeInfo.getAllSupertypes()) {
			partialRelationInfoMap.get(superType).assertions().mergeValue(tuple, TruthValue.TRUE);
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
		} else if (predicateDefinition.getKind() == PredicateKind.SHADOW) {
			collectShadowPredicateDefinition(predicateDefinition, storeBuilder);
		} else {
			collectComputedPredicateDefinition(predicateDefinition, storeBuilder);
		}
	}

	private void collectComputedPredicateDefinition(PredicateDefinition predicateDefinition,
													ModelStoreBuilder storeBuilder) {
		var partialRelation = getPartialRelation(predicateDefinition);
		var query = queryCompiler.toQuery(partialRelation.name(), predicateDefinition);
		boolean mutable = mutableRelationCollector.isMutable(predicateDefinition);
		TruthValue defaultValue;
		if (predicateDefinition.getKind() == PredicateKind.ERROR) {
			defaultValue = TruthValue.FALSE;
		} else {
			var seed = modelSeed.getSeed(partialRelation);
			defaultValue = seed.majorityValue() == TruthValue.FALSE ? TruthValue.FALSE : TruthValue.UNKNOWN;
			var cursor = seed.getCursor(defaultValue, problemTrace.getNodeTrace().size());
			// The symbol should be mutable if there is at least one non-default entry in the seed.
			mutable = mutable || cursor.move();
		}
		var parameterTypes = getParameterTypes(predicateDefinition, null);
		var supersets = getSupersets(predicateDefinition);
		var translator = new PredicateTranslator(partialRelation, query, parameterTypes, supersets, mutable,
				defaultValue);
		storeBuilder.with(translator);
		var computedPredicate = predicateDefinition.getComputedValue();
		if (computedPredicate != null) {
			var computedPartialRelation = getPartialRelation(computedPredicate);
			// Always keep the interpretation of computed predicates, because they are used in solution serialization.
			// This shouldn't add an overhead, because the lifted versions of the computed predicate are used in the
			// computation of the interpretation of the original predicate, too. The exceptions with overhead are the
			// error predicates, which can be safely ignored during serialization of consistent models.
			var hasComputedInterpretation = !ProblemUtil.isError(predicateDefinition) || keepShadowPredicates;
			var computedTranslator = new ShadowPredicateTranslator(computedPartialRelation, query,
					hasComputedInterpretation);
			storeBuilder.with(computedTranslator);
		}
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

	private Set<PartialRelation> getSupersets(PredicateDefinition predicateDefinition) {
		return predicateDefinition.getSuperSets().stream()
				.map(this::getPartialRelation)
				.collect(Collectors.toUnmodifiableSet());
	}

	private void collectBasePredicateDefinition(PredicateDefinition predicateDefinition,
												ModelStoreBuilder storeBuilder) {
		var partialRelation = getPartialRelation(predicateDefinition);
		var parameterTypes = getParameterTypes(predicateDefinition, nodeRelation);
		var supersets = getSupersets(predicateDefinition);
		var seed = modelSeed.getSeed(partialRelation);
		var defaultValue = seed.majorityValue() == TruthValue.FALSE ? TruthValue.FALSE : TruthValue.UNKNOWN;
		var concretizationSettings = getConcretizationSettings(predicateDefinition);
		var translator = new BasePredicateTranslator(partialRelation, parameterTypes, supersets, defaultValue,
				concretizationSettings);
		storeBuilder.with(translator);
	}

	private void collectShadowPredicateDefinition(PredicateDefinition predicateDefinition,
												  ModelStoreBuilder storeBuilder) {
		var partialRelation = getPartialRelation(predicateDefinition);
		var query = queryCompiler.toQuery(partialRelation.name(), predicateDefinition);
		var translator = new ShadowPredicateTranslator(partialRelation, query, keepShadowPredicates);
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

	public void setKeepNonExistingObjects(boolean keepNonExistingObjects) {
		this.keepNonExistingObjects = keepNonExistingObjects;
	}

	public boolean isKeepShadowPredicates() {
		return keepShadowPredicates;
	}

	public void setKeepShadowPredicates(boolean keepShadowPredicates) {
		this.keepShadowPredicates = keepShadowPredicates;
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

	private record FunctionInfo<A extends AbstractValue<A, C>, C>(PartialFunction<A, C> partialFunction,
																  AbstractDomain<A, C> abstractDomain,
																  MutableSeed<A> assertions,
																  MutableSeed<A> defaultAssertions) {
		public FunctionInfo(PartialFunction<A, C> partialFunction, AbstractDomain<A, C> abstractDomain) {
			this(partialFunction, abstractDomain, MutableSeed.of(partialFunction.arity(), abstractDomain, null),
					MutableSeed.of(partialFunction.arity(), abstractDomain, abstractDomain.unknown()));
		}

		public Seed<A> toSeed() {
			defaultAssertions.overwriteValues(assertions);
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
				problemTrace.putRuleDefinition(ruleDefinition, rule.rule());
				storeBuilder.tryGetAdapter(DesignSpaceExplorationBuilder.class)
						.ifPresent(dseBuilder -> dseBuilder.transformation(rule));
			}
			case PROPAGATION -> {
				var rules = new ArrayList<Rule>();
				var propagationRules = ruleCompiler.toPropagationRules(name, ruleDefinition,
						ConcretenessSpecification.PARTIAL);
				var concretizationRules = ruleCompiler.toPropagationRules(name, ruleDefinition,
						ConcretenessSpecification.CANDIDATE);
				rules.addAll(propagationRules);
				rules.addAll(concretizationRules);
				problemTrace.putPropagationRuleDefinition(ruleDefinition, List.copyOf(rules));
				storeBuilder.tryGetAdapter(PropagationBuilder.class).ifPresent(propagationBuilder -> {
					propagationBuilder.rules(propagationRules);
					propagationBuilder.concretizationRules(concretizationRules);
				});
			}
			case CONCRETIZATION -> {
				var rules = ruleCompiler.toPropagationRules(name, ruleDefinition, ConcretenessSpecification.CANDIDATE);
				problemTrace.putPropagationRuleDefinition(ruleDefinition, rules);
				storeBuilder.tryGetAdapter(PropagationBuilder.class)
						.ifPresent(propagationBuilder -> propagationBuilder.concretizationRules(rules));
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
