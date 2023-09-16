/*******************************************************************************
 * Copyright (c) 2010-2014, Bergmann Gabor, Istvan Rath and Daniel Varro
 * Copyright (c) 2023 The Refinery Authors <https://refinery.tools>
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.interpreter.rete.construction.plancompiler;

import org.apache.log4j.Logger;
import tools.refinery.interpreter.matchers.InterpreterRuntimeException;
import tools.refinery.interpreter.matchers.backend.CommonQueryHintOptions;
import tools.refinery.interpreter.matchers.backend.IQueryBackendHintProvider;
import tools.refinery.interpreter.matchers.backend.QueryEvaluationHint;
import tools.refinery.interpreter.matchers.context.IInputKey;
import tools.refinery.interpreter.matchers.context.IPosetComparator;
import tools.refinery.interpreter.matchers.context.IQueryCacheContext;
import tools.refinery.interpreter.matchers.context.IQueryMetaContext;
import tools.refinery.interpreter.matchers.planning.IQueryPlannerStrategy;
import tools.refinery.interpreter.matchers.planning.SubPlan;
import tools.refinery.interpreter.matchers.planning.helpers.BuildHelper;
import tools.refinery.interpreter.matchers.planning.operations.*;
import tools.refinery.interpreter.matchers.psystem.*;
import tools.refinery.interpreter.matchers.psystem.aggregations.IMultisetAggregationOperator;
import tools.refinery.interpreter.matchers.psystem.analysis.QueryAnalyzer;
import tools.refinery.interpreter.matchers.psystem.basicdeferred.*;
import tools.refinery.interpreter.matchers.psystem.basicenumerables.*;
import tools.refinery.interpreter.matchers.psystem.queries.PParameter;
import tools.refinery.interpreter.matchers.psystem.queries.PQuery;
import tools.refinery.interpreter.matchers.psystem.queries.PVisibility;
import tools.refinery.interpreter.matchers.psystem.rewriters.*;
import tools.refinery.interpreter.matchers.tuple.Tuple;
import tools.refinery.interpreter.matchers.tuple.TupleMask;
import tools.refinery.interpreter.matchers.tuple.Tuples;
import tools.refinery.interpreter.matchers.util.CollectionsFactory;
import tools.refinery.interpreter.matchers.util.CollectionsFactory.MemoryType;
import tools.refinery.interpreter.matchers.util.IMultiLookup;
import tools.refinery.interpreter.rete.matcher.TimelyConfiguration;
import tools.refinery.interpreter.rete.recipes.*;
import tools.refinery.interpreter.rete.recipes.helper.RecipesHelper;
import tools.refinery.interpreter.rete.traceability.*;
import tools.refinery.interpreter.rete.util.ReteHintOptions;

import java.util.*;
import java.util.Map.Entry;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Compiles queries and query plans into Rete recipes, traced by respectively a {@link CompiledQuery} or
 * {@link CompiledSubPlan}.
 *
 * @author Bergmann Gabor
 *
 */
public class ReteRecipeCompiler {

    private final IQueryPlannerStrategy plannerStrategy;
    private final IQueryMetaContext metaContext;
    private final IQueryBackendHintProvider hintProvider;
    private final PDisjunctionRewriter normalizer;
    private final QueryAnalyzer queryAnalyzer;
    private final Logger logger;

    /**
     * @since 2.2
     */
    protected final boolean deleteAndRederiveEvaluation;
    /**
     * @since 2.4
     */
    protected final TimelyConfiguration timelyEvaluation;

    /**
     * @since 1.5
     */
    public ReteRecipeCompiler(IQueryPlannerStrategy plannerStrategy, Logger logger, IQueryMetaContext metaContext,
            IQueryCacheContext queryCacheContext, IQueryBackendHintProvider hintProvider, QueryAnalyzer queryAnalyzer) {
        this(plannerStrategy, logger, metaContext, queryCacheContext, hintProvider, queryAnalyzer, false, null);
    }

    /**
     * @since 2.4
     */
    public ReteRecipeCompiler(IQueryPlannerStrategy plannerStrategy, Logger logger, IQueryMetaContext metaContext,
            IQueryCacheContext queryCacheContext, IQueryBackendHintProvider hintProvider, QueryAnalyzer queryAnalyzer,
            boolean deleteAndRederiveEvaluation, TimelyConfiguration timelyEvaluation) {
        super();
        this.deleteAndRederiveEvaluation = deleteAndRederiveEvaluation;
        this.timelyEvaluation = timelyEvaluation;
        this.plannerStrategy = plannerStrategy;
        this.logger = logger;
        this.metaContext = metaContext;
        this.queryAnalyzer = queryAnalyzer;
        this.normalizer = new PDisjunctionRewriterCacher(new SurrogateQueryRewriter(),
                new PBodyNormalizer(metaContext) {

                    @Override
                    protected boolean shouldExpandWeakenedAlternatives(PQuery query) {
                        QueryEvaluationHint hint = ReteRecipeCompiler.this.hintProvider.getQueryEvaluationHint(query);
                        Boolean expandWeakenedAlternativeConstraints = ReteHintOptions.expandWeakenedAlternativeConstraints
                                .getValueOrDefault(hint);
                        return expandWeakenedAlternativeConstraints;
                    }

                });
        this.hintProvider = hintProvider;
    }

    static final RecipesFactory FACTORY = RecipesFactory.eINSTANCE;

    // INTERNALLY CACHED
    private Map<PBody, SubPlan> plannerCache = new HashMap<PBody, SubPlan>();
    private Set<PBody> planningInProgress = new HashSet<PBody>();

    private Map<PQuery, CompiledQuery> queryCompilerCache = new HashMap<PQuery, CompiledQuery>();
    private Set<PQuery> compilationInProgress = new HashSet<PQuery>();
    private IMultiLookup<PQuery, RecursionCutoffPoint> recursionCutoffPoints = CollectionsFactory.createMultiLookup(Object.class, MemoryType.SETS, Object.class);
    private Map<SubPlan, CompiledSubPlan> subPlanCompilerCache = new HashMap<SubPlan, CompiledSubPlan>();
    private Map<ReteNodeRecipe, SubPlan> compilerBackTrace = new HashMap<ReteNodeRecipe, SubPlan>();

    /**
     * Clears internal state
     */
    public void reset() {
        plannerCache.clear();
        planningInProgress.clear();
        queryCompilerCache.clear();
        subPlanCompilerCache.clear();
        compilerBackTrace.clear();
    }

    /**
     * Returns a {@link CompiledQuery} compiled from a query
     * @throws InterpreterRuntimeException
     */
    public CompiledQuery getCompiledForm(PQuery query) {
        CompiledQuery compiled = queryCompilerCache.get(query);
        if (compiled == null) {

            IRewriterTraceCollector traceCollector = CommonQueryHintOptions.normalizationTraceCollector
                    .getValueOrDefault(hintProvider.getQueryEvaluationHint(query));
            if (traceCollector != null) {
                traceCollector.addTrace(query, query);
            }

            boolean reentrant = !compilationInProgress.add(query);
            if (reentrant) { // oops, recursion into body in progress
                RecursionCutoffPoint cutoffPoint = new RecursionCutoffPoint(query, getHints(query), metaContext,
                        deleteAndRederiveEvaluation, timelyEvaluation);
                recursionCutoffPoints.addPair(query, cutoffPoint);
                return cutoffPoint.getCompiledQuery();
            } else { // not reentrant, therefore no recursion, do the compilation
                try {
                    compiled = compileProduction(query);
                    queryCompilerCache.put(query, compiled);
                    // backTrace.put(compiled.getRecipe(), plan);

                    // if this was a recursive query, mend all points where recursion was cut off
                    for (RecursionCutoffPoint cutoffPoint : recursionCutoffPoints.lookupOrEmpty(query)) {
                        cutoffPoint.mend(compiled);
                    }
                } finally {
                    compilationInProgress.remove(query);
                }
            }
        }
        return compiled;
    }

    /**
     * Returns a {@link CompiledSubPlan} compiled from a query plan
     * @throws InterpreterRuntimeException
     */
    public CompiledSubPlan getCompiledForm(SubPlan plan) {
        CompiledSubPlan compiled = subPlanCompilerCache.get(plan);
        if (compiled == null) {
            compiled = doCompileDispatch(plan);
            subPlanCompilerCache.put(plan, compiled);
            compilerBackTrace.put(compiled.getRecipe(), plan);
        }
        return compiled;
    }

    /**
     * @throws InterpreterRuntimeException
     */
    public SubPlan getPlan(PBody pBody) {
        // if the query is not marked as being compiled, initiate compilation
        // (this is useful in case of recursion if getPlan() is the entry point)
        PQuery pQuery = pBody.getPattern();
        if (!compilationInProgress.contains(pQuery))
            getCompiledForm(pQuery);

        // Is the plan already cached?
        SubPlan plan = plannerCache.get(pBody);
        if (plan == null) {
            boolean reentrant = !planningInProgress.add(pBody);
            if (reentrant) { // oops, recursion into body in progress
                throw new IllegalArgumentException(
                        "Planning-level recursion unsupported: " + pBody.getPattern().getFullyQualifiedName());
            } else { // not reentrant, therefore no recursion, do the planning
                try {
                    plan = plannerStrategy.plan(pBody, logger, metaContext);
                    plannerCache.put(pBody, plan);
                } finally {
                    planningInProgress.remove(pBody);
                }
            }
        }
        return plan;
    }

    private CompiledQuery compileProduction(PQuery query) {
        Collection<SubPlan> bodyPlans = new ArrayList<SubPlan>();
        normalizer.setTraceCollector(CommonQueryHintOptions.normalizationTraceCollector
                .getValueOrDefault(hintProvider.getQueryEvaluationHint(query)));
        for (PBody pBody : normalizer.rewrite(query).getBodies()) {
            SubPlan bodyPlan = getPlan(pBody);
            bodyPlans.add(bodyPlan);
        }
        return doCompileProduction(query, bodyPlans);
    }

    private CompiledQuery doCompileProduction(PQuery query, Collection<SubPlan> bodies) {
        // TODO skip production node if there is just one body and no projection needed?
        Map<PBody, RecipeTraceInfo> bodyFinalTraces = new HashMap<PBody, RecipeTraceInfo>();
        Collection<ReteNodeRecipe> bodyFinalRecipes = new HashSet<ReteNodeRecipe>();

        for (SubPlan bodyFinalPlan : bodies) {
            // skip over any projections at the end
            bodyFinalPlan = BuildHelper.eliminateTrailingProjections(bodyFinalPlan);

            // TODO checkAndTrimEqualVariables may introduce superfluous trim,
            // but whatever (no uniqueness enforcer needed)

            // compile body
            final CompiledSubPlan compiledBody = getCompiledForm(bodyFinalPlan);

            // project to parameter list
            RecipeTraceInfo finalTrace = projectBodyFinalToParameters(compiledBody, false);

            bodyFinalTraces.put(bodyFinalPlan.getBody(), finalTrace);
            bodyFinalRecipes.add(finalTrace.getRecipe());
        }

        CompiledQuery compiled = CompilerHelper.makeQueryTrace(query, bodyFinalTraces, bodyFinalRecipes,
                getHints(query), metaContext, deleteAndRederiveEvaluation, timelyEvaluation);

        return compiled;
    }

    private CompiledSubPlan doCompileDispatch(SubPlan plan) {
        final POperation operation = plan.getOperation();
        if (operation instanceof PEnumerate) {
            return doCompileEnumerate(((PEnumerate) operation).getEnumerablePConstraint(), plan);
        } else if (operation instanceof PApply) {
            final PConstraint pConstraint = ((PApply) operation).getPConstraint();
            if (pConstraint instanceof EnumerablePConstraint) {
                CompiledSubPlan primaryParent = getCompiledForm(plan.getParentPlans().get(0));
                PlanningTrace secondaryParent = doEnumerateDispatch(plan, (EnumerablePConstraint) pConstraint);
                return compileToNaturalJoin(plan, primaryParent, secondaryParent);
            } else if (pConstraint instanceof DeferredPConstraint) {
                return doDeferredDispatch((DeferredPConstraint) pConstraint, plan);
            } else {
                throw new IllegalArgumentException("Unsupported PConstraint in query plan: " + plan.toShortString());
            }
        } else if (operation instanceof PJoin) {
            return doCompileJoin((PJoin) operation, plan);
        } else if (operation instanceof PProject) {
            return doCompileProject((PProject) operation, plan);
        } else if (operation instanceof PStart) {
            return doCompileStart((PStart) operation, plan);
        } else {
            throw new IllegalArgumentException("Unsupported POperation in query plan: " + plan.toShortString());
        }
    }

    private CompiledSubPlan doDeferredDispatch(DeferredPConstraint constraint, SubPlan plan) {
        final SubPlan parentPlan = plan.getParentPlans().get(0);
        final CompiledSubPlan parentCompiled = getCompiledForm(parentPlan);
        if (constraint instanceof Equality) {
            return compileDeferred((Equality) constraint, plan, parentPlan, parentCompiled);
        } else if (constraint instanceof ExportedParameter) {
            return compileDeferred((ExportedParameter) constraint, plan, parentPlan, parentCompiled);
        } else if (constraint instanceof Inequality) {
            return compileDeferred((Inequality) constraint, plan, parentPlan, parentCompiled);
        } else if (constraint instanceof NegativePatternCall) {
            return compileDeferred((NegativePatternCall) constraint, plan, parentPlan, parentCompiled);
        } else if (constraint instanceof PatternMatchCounter) {
            return compileDeferred((PatternMatchCounter) constraint, plan, parentPlan, parentCompiled);
        } else if (constraint instanceof AggregatorConstraint) {
            return compileDeferred((AggregatorConstraint) constraint, plan, parentPlan, parentCompiled);
        } else if (constraint instanceof ExpressionEvaluation) {
            return compileDeferred((ExpressionEvaluation) constraint, plan, parentPlan, parentCompiled);
        } else if (constraint instanceof TypeFilterConstraint) {
            return compileDeferred((TypeFilterConstraint) constraint, plan, parentPlan, parentCompiled);
        }
        throw new UnsupportedOperationException("Unknown deferred constraint " + constraint);
    }

    private CompiledSubPlan compileDeferred(Equality constraint, SubPlan plan, SubPlan parentPlan,
            CompiledSubPlan parentCompiled) {
        if (constraint.isMoot())
            return parentCompiled.cloneFor(plan);

        Integer index1 = parentCompiled.getPosMapping().get(constraint.getWho());
        Integer index2 = parentCompiled.getPosMapping().get(constraint.getWithWhom());

        if (index1 != null && index2 != null && index1.intValue() != index2.intValue()) {
            Integer indexLower = Math.min(index1, index2);
            Integer indexHigher = Math.max(index1, index2);

            EqualityFilterRecipe equalityFilterRecipe = FACTORY.createEqualityFilterRecipe();
            equalityFilterRecipe.setParent(parentCompiled.getRecipe());
            equalityFilterRecipe.getIndices().add(indexLower);
            equalityFilterRecipe.getIndices().add(indexHigher);

            return new CompiledSubPlan(plan, parentCompiled.getVariablesTuple(), equalityFilterRecipe, parentCompiled);
        } else {
            throw new IllegalArgumentException(String.format("Unable to interpret %s after compiled parent %s",
                    plan.toShortString(), parentCompiled.toString()));
        }
    }

    /**
     * Precondition: constantTrace must map to a ConstantRecipe, and all of its variables must be contained in
     * toFilterTrace.
     */
    private CompiledSubPlan compileConstantFiltering(SubPlan plan, PlanningTrace toFilterTrace,
            ConstantRecipe constantRecipe, List<PVariable> filteredVariables) {
        PlanningTrace resultTrace = toFilterTrace;

        int constantVariablesSize = filteredVariables.size();
        for (int i = 0; i < constantVariablesSize; ++i) {
            Object constantValue = constantRecipe.getConstantValues().get(i);
            PVariable filteredVariable = filteredVariables.get(i);
            int filteredColumn = resultTrace.getVariablesTuple().indexOf(filteredVariable);

            DiscriminatorDispatcherRecipe dispatcherRecipe = FACTORY.createDiscriminatorDispatcherRecipe();
            dispatcherRecipe.setDiscriminationColumnIndex(filteredColumn);
            dispatcherRecipe.setParent(resultTrace.getRecipe());

            PlanningTrace dispatcherTrace = new PlanningTrace(plan, resultTrace.getVariablesTuple(), dispatcherRecipe,
                    resultTrace);

            DiscriminatorBucketRecipe bucketRecipe = FACTORY.createDiscriminatorBucketRecipe();
            bucketRecipe.setBucketKey(constantValue);
            bucketRecipe.setParent(dispatcherRecipe);

            PlanningTrace bucketTrace = new PlanningTrace(plan, dispatcherTrace.getVariablesTuple(), bucketRecipe,
                    dispatcherTrace);

            resultTrace = bucketTrace;
        }

        return resultTrace.cloneFor(plan);
    }

    private CompiledSubPlan compileDeferred(ExportedParameter constraint, SubPlan plan, SubPlan parentPlan,
            CompiledSubPlan parentCompiled) {
        return parentCompiled.cloneFor(plan);
    }

    private CompiledSubPlan compileDeferred(Inequality constraint, SubPlan plan, SubPlan parentPlan,
            CompiledSubPlan parentCompiled) {
        if (constraint.isEliminable())
            return parentCompiled.cloneFor(plan);

        Integer index1 = parentCompiled.getPosMapping().get(constraint.getWho());
        Integer index2 = parentCompiled.getPosMapping().get(constraint.getWithWhom());

        if (index1 != null && index2 != null && index1.intValue() != index2.intValue()) {
            Integer indexLower = Math.min(index1, index2);
            Integer indexHigher = Math.max(index1, index2);

            InequalityFilterRecipe inequalityFilterRecipe = FACTORY.createInequalityFilterRecipe();
            inequalityFilterRecipe.setParent(parentCompiled.getRecipe());
            inequalityFilterRecipe.setSubject(indexLower);
            inequalityFilterRecipe.getInequals().add(indexHigher);

            return new CompiledSubPlan(plan, parentCompiled.getVariablesTuple(), inequalityFilterRecipe,
                    parentCompiled);
        } else {
            throw new IllegalArgumentException(String.format("Unable to interpret %s after compiled parent %s",
                    plan.toShortString(), parentCompiled.toString()));
        }
    }

    private CompiledSubPlan compileDeferred(TypeFilterConstraint constraint, SubPlan plan, SubPlan parentPlan,
            CompiledSubPlan parentCompiled) {
        final IInputKey inputKey = constraint.getInputKey();
        if (!metaContext.isStateless(inputKey))
            throw new UnsupportedOperationException(
                    "Non-enumerable input keys are currently supported in Rete only if they are stateless, unlike "
                            + inputKey);

        final Tuple constraintVariables = constraint.getVariablesTuple();
        final List<PVariable> parentVariables = parentCompiled.getVariablesTuple();

        Mask mask; // select elements of the tuple to check against extensional relation
        if (Tuples.flatTupleOf(parentVariables.toArray()).equals(constraintVariables)) {
            mask = null; // lucky case, parent signature equals that of input key
        } else {
            List<PVariable> variables = new ArrayList<PVariable>();
            for (Object variable : constraintVariables.getElements()) {
                variables.add((PVariable) variable);
            }
            mask = CompilerHelper.makeProjectionMask(parentCompiled, variables);
        }
        InputFilterRecipe inputFilterRecipe = RecipesHelper.inputFilterRecipe(parentCompiled.getRecipe(), inputKey,
                inputKey.getStringID(), mask);
        return new CompiledSubPlan(plan, parentVariables, inputFilterRecipe, parentCompiled);
    }

    private CompiledSubPlan compileDeferred(NegativePatternCall constraint, SubPlan plan, SubPlan parentPlan,
            CompiledSubPlan parentCompiled) {
        final PlanningTrace callTrace = referQuery(constraint.getReferredQuery(), plan,
                constraint.getActualParametersTuple());

        CompilerHelper.JoinHelper joinHelper = new CompilerHelper.JoinHelper(plan, parentCompiled, callTrace);
        final RecipeTraceInfo primaryIndexer = joinHelper.getPrimaryIndexer();
        final RecipeTraceInfo secondaryIndexer = joinHelper.getSecondaryIndexer();

        AntiJoinRecipe antiJoinRecipe = FACTORY.createAntiJoinRecipe();
        antiJoinRecipe.setLeftParent((ProjectionIndexerRecipe) primaryIndexer.getRecipe());
        antiJoinRecipe.setRightParent((IndexerRecipe) secondaryIndexer.getRecipe());

        return new CompiledSubPlan(plan, parentCompiled.getVariablesTuple(), antiJoinRecipe, primaryIndexer,
                secondaryIndexer);
    }

    private CompiledSubPlan compileDeferred(PatternMatchCounter constraint, SubPlan plan, SubPlan parentPlan,
            CompiledSubPlan parentCompiled) {
        final PlanningTrace callTrace = referQuery(constraint.getReferredQuery(), plan,
                constraint.getActualParametersTuple());

        // hack: use some mask computations (+ the indexers) from a fake natural join against the called query
        CompilerHelper.JoinHelper fakeJoinHelper = new CompilerHelper.JoinHelper(plan, parentCompiled, callTrace);
        final RecipeTraceInfo primaryIndexer = fakeJoinHelper.getPrimaryIndexer();
        final RecipeTraceInfo callProjectionIndexer = fakeJoinHelper.getSecondaryIndexer();

        final List<PVariable> sideVariablesTuple =  new ArrayList<PVariable>(
                fakeJoinHelper.getSecondaryMask().transform(callTrace.getVariablesTuple()));
        /* if (!booleanCheck) */ sideVariablesTuple.add(constraint.getResultVariable());

        CountAggregatorRecipe aggregatorRecipe = FACTORY.createCountAggregatorRecipe();
        aggregatorRecipe.setParent((ProjectionIndexerRecipe) callProjectionIndexer.getRecipe());
        PlanningTrace aggregatorTrace = new PlanningTrace(plan, sideVariablesTuple, aggregatorRecipe,
                callProjectionIndexer);

        IndexerRecipe aggregatorIndexerRecipe = FACTORY.createAggregatorIndexerRecipe();
        aggregatorIndexerRecipe.setParent(aggregatorRecipe);
        // aggregatorIndexerRecipe.setMask(RecipesHelper.mask(
        // sideVariablesTuple.size(),
        // //use same indices as in the projection indexer
        // // EVEN if result variable already visible in left parent
        // fakeJoinHelper.getSecondaryMask().indices
        // ));

        int aggregatorWidth = sideVariablesTuple.size();
        int aggregateResultIndex = aggregatorWidth - 1;

        aggregatorIndexerRecipe.setMask(CompilerHelper.toRecipeMask(TupleMask.omit(
                // aggregate according all but the last index
                aggregateResultIndex, aggregatorWidth)));
        PlanningTrace aggregatorIndexerTrace = new PlanningTrace(plan, sideVariablesTuple, aggregatorIndexerRecipe,
                aggregatorTrace);

        JoinRecipe naturalJoinRecipe = FACTORY.createJoinRecipe();
        naturalJoinRecipe.setLeftParent((ProjectionIndexerRecipe) primaryIndexer.getRecipe());
        naturalJoinRecipe.setRightParent(aggregatorIndexerRecipe);
        naturalJoinRecipe.setRightParentComplementaryMask(RecipesHelper.mask(aggregatorWidth,
                // extend with last element only - the computation value
                aggregateResultIndex));

        // what if the new variable already has a value?
        // even if already known, we add the new result variable, so that it can be filtered at the end
        // boolean alreadyKnown = parentPlan.getVisibleVariables().contains(constraint.getResultVariable());

        final List<PVariable> aggregatedVariablesTuple = new ArrayList<PVariable>(parentCompiled.getVariablesTuple());
        aggregatedVariablesTuple.add(constraint.getResultVariable());

        PlanningTrace joinTrace = new PlanningTrace(plan, aggregatedVariablesTuple, naturalJoinRecipe, primaryIndexer,
                aggregatorIndexerTrace);

        return CompilerHelper.checkAndTrimEqualVariables(plan, joinTrace).cloneFor(plan);
        // if (!alreadyKnown) {
        // return joinTrace.cloneFor(plan);
        // } else {
        // //final Integer equalsWithIndex = parentCompiled.getPosMapping().get(parentCompiled.getVariablesTuple());
        // }
    }

    private CompiledSubPlan compileDeferred(AggregatorConstraint constraint, SubPlan plan, SubPlan parentPlan,
            CompiledSubPlan parentCompiled) {
        final PlanningTrace callTrace = referQuery(constraint.getReferredQuery(), plan,
                constraint.getActualParametersTuple());

        // hack: use some mask computations (+ the indexers) from a fake natural join against the called query
        CompilerHelper.JoinHelper fakeJoinHelper = new CompilerHelper.JoinHelper(plan, parentCompiled, callTrace);
        final RecipeTraceInfo primaryIndexer = fakeJoinHelper.getPrimaryIndexer();
        TupleMask callGroupMask = fakeJoinHelper.getSecondaryMask();

        final List<PVariable> sideVariablesTuple = new ArrayList<PVariable>(
                callGroupMask.transform(callTrace.getVariablesTuple()));
        /* if (!booleanCheck) */ sideVariablesTuple.add(constraint.getResultVariable());

        IMultisetAggregationOperator<?, ?, ?> operator = constraint.getAggregator().getOperator();

        SingleColumnAggregatorRecipe columnAggregatorRecipe = FACTORY.createSingleColumnAggregatorRecipe();
        columnAggregatorRecipe.setParent(callTrace.getRecipe());
        columnAggregatorRecipe.setMultisetAggregationOperator(operator);

        int columnIndex = constraint.getAggregatedColumn();
        IPosetComparator posetComparator = null;
        Mask groupMask = CompilerHelper.toRecipeMask(callGroupMask);

        // temporary solution to support the deprecated option for now
        final boolean deleteAndRederiveEvaluationDep = this.deleteAndRederiveEvaluation || ReteHintOptions.deleteRederiveEvaluation.getValueOrDefault(getHints(plan));

        columnAggregatorRecipe.setDeleteRederiveEvaluation(deleteAndRederiveEvaluationDep);
        if (deleteAndRederiveEvaluationDep || (this.timelyEvaluation != null)) {
            List<PParameter> parameters = constraint.getReferredQuery().getParameters();
            IInputKey key = parameters.get(columnIndex).getDeclaredUnaryType();
            if (key != null && metaContext.isPosetKey(key)) {
                posetComparator = metaContext.getPosetComparator(Collections.singleton(key));
            }
        }

        if (posetComparator == null) {
            columnAggregatorRecipe.setGroupByMask(groupMask);
            columnAggregatorRecipe.setAggregableIndex(columnIndex);
        } else {
            MonotonicityInfo monotonicityInfo = FACTORY.createMonotonicityInfo();
            monotonicityInfo.setCoreMask(groupMask);
            monotonicityInfo.setPosetMask(CompilerHelper.toRecipeMask(
                    TupleMask.selectSingle(columnIndex, constraint.getActualParametersTuple().getSize())));
            monotonicityInfo.setPosetComparator(posetComparator);
            columnAggregatorRecipe.setOptionalMonotonicityInfo(monotonicityInfo);
        }

        ReteNodeRecipe aggregatorRecipe = columnAggregatorRecipe;
        PlanningTrace aggregatorTrace = new PlanningTrace(plan, sideVariablesTuple, aggregatorRecipe, callTrace);

        IndexerRecipe aggregatorIndexerRecipe = FACTORY.createAggregatorIndexerRecipe();
        aggregatorIndexerRecipe.setParent(aggregatorRecipe);

        int aggregatorWidth = sideVariablesTuple.size();
        int aggregateResultIndex = aggregatorWidth - 1;

        aggregatorIndexerRecipe.setMask(CompilerHelper.toRecipeMask(TupleMask.omit(
                // aggregate according all but the last index
                aggregateResultIndex, aggregatorWidth)));
        PlanningTrace aggregatorIndexerTrace = new PlanningTrace(plan, sideVariablesTuple, aggregatorIndexerRecipe,
                aggregatorTrace);

        JoinRecipe naturalJoinRecipe = FACTORY.createJoinRecipe();
        naturalJoinRecipe.setLeftParent((ProjectionIndexerRecipe) primaryIndexer.getRecipe());
        naturalJoinRecipe.setRightParent(aggregatorIndexerRecipe);
        naturalJoinRecipe.setRightParentComplementaryMask(RecipesHelper.mask(aggregatorWidth,
                // extend with last element only - the computation value
                aggregateResultIndex));

        // what if the new variable already has a value?
        // even if already known, we add the new result variable, so that it can be filtered at the end
        // boolean alreadyKnown = parentPlan.getVisibleVariables().contains(constraint.getResultVariable());

        final List<PVariable> finalVariablesTuple = new ArrayList<PVariable>(parentCompiled.getVariablesTuple());
        finalVariablesTuple.add(constraint.getResultVariable());

        PlanningTrace joinTrace = new PlanningTrace(plan, finalVariablesTuple, naturalJoinRecipe, primaryIndexer,
                aggregatorIndexerTrace);

        return CompilerHelper.checkAndTrimEqualVariables(plan, joinTrace).cloneFor(plan);
        // if (!alreadyKnown) {
        // return joinTrace.cloneFor(plan);
        // } else {
        // //final Integer equalsWithIndex = parentCompiled.getPosMapping().get(parentCompiled.getVariablesTuple());
        // }
    }

    private CompiledSubPlan compileDeferred(ExpressionEvaluation constraint, SubPlan plan, SubPlan parentPlan,
            CompiledSubPlan parentCompiled) {
        Map<String, Integer> tupleNameMap = new HashMap<String, Integer>();
        for (String name : constraint.getEvaluator().getInputParameterNames()) {
            Map<? extends Object, Integer> index = parentCompiled.getPosMapping();
            PVariable variable = constraint.getPSystem().getVariableByNameChecked(name);
            Integer position = index.get(variable);
            tupleNameMap.put(name, position);
        }

        final PVariable outputVariable = constraint.getOutputVariable();
        final boolean booleanCheck = outputVariable == null;

        // TODO determine whether expression is costly
        boolean cacheOutput = ReteHintOptions.cacheOutputOfEvaluatorsByDefault.getValueOrDefault(getHints(plan));
        // for (PAnnotation pAnnotation :
        // plan.getBody().getPattern().getAnnotationsByName(EXPRESSION_EVALUATION_ANNOTATION"")) {
        // for (Object value : pAnnotation.getAllValues("expensive")) {
        // if (value instanceof Boolean)
        // cacheOutput = (boolean) value;
        // }
        // }

        ExpressionEnforcerRecipe enforcerRecipe = booleanCheck ? FACTORY.createCheckRecipe()
                : FACTORY.createEvalRecipe();
        enforcerRecipe.setParent(parentCompiled.getRecipe());
        enforcerRecipe.setExpression(RecipesHelper.expressionDefinition(constraint.getEvaluator()));
        enforcerRecipe.setCacheOutput(cacheOutput);
        if (enforcerRecipe instanceof EvalRecipe) {
            ((EvalRecipe) enforcerRecipe).setUnwinding(constraint.isUnwinding());
        }
        for (Entry<String, Integer> entry : tupleNameMap.entrySet()) {
            enforcerRecipe.getMappedIndices().put(entry.getKey(), entry.getValue());
        }

        final List<PVariable> enforcerVariablesTuple = new ArrayList<PVariable>(parentCompiled.getVariablesTuple());
        if (!booleanCheck)
            enforcerVariablesTuple.add(outputVariable);
        PlanningTrace enforcerTrace = new PlanningTrace(plan, enforcerVariablesTuple, enforcerRecipe, parentCompiled);

        return CompilerHelper.checkAndTrimEqualVariables(plan, enforcerTrace).cloneFor(plan);
    }

    private CompiledSubPlan doCompileJoin(PJoin operation, SubPlan plan) {
        final List<CompiledSubPlan> compiledParents = getCompiledFormOfParents(plan);
        final CompiledSubPlan leftCompiled = compiledParents.get(0);
        final CompiledSubPlan rightCompiled = compiledParents.get(1);

        return compileToNaturalJoin(plan, leftCompiled, rightCompiled);
    }

    private CompiledSubPlan compileToNaturalJoin(SubPlan plan, final PlanningTrace leftCompiled,
            final PlanningTrace rightCompiled) {
        // CHECK IF SPECIAL CASE

        // Is constant filtering applicable?
        if (ReteHintOptions.useDiscriminatorDispatchersForConstantFiltering.getValueOrDefault(getHints(plan))) {
            if (leftCompiled.getRecipe() instanceof ConstantRecipe
                    && rightCompiled.getVariablesTuple().containsAll(leftCompiled.getVariablesTuple())) {
                return compileConstantFiltering(plan, rightCompiled, (ConstantRecipe) leftCompiled.getRecipe(),
                        leftCompiled.getVariablesTuple());
            }
            if (rightCompiled.getRecipe() instanceof ConstantRecipe
                    && leftCompiled.getVariablesTuple().containsAll(rightCompiled.getVariablesTuple())) {
                return compileConstantFiltering(plan, leftCompiled, (ConstantRecipe) rightCompiled.getRecipe(),
                        rightCompiled.getVariablesTuple());
            }
        }

        // ELSE: ACTUAL JOIN
        CompilerHelper.JoinHelper joinHelper = new CompilerHelper.JoinHelper(plan, leftCompiled, rightCompiled);
        return new CompiledSubPlan(plan, joinHelper.getNaturalJoinVariablesTuple(), joinHelper.getNaturalJoinRecipe(),
                joinHelper.getPrimaryIndexer(), joinHelper.getSecondaryIndexer());
    }

    private CompiledSubPlan doCompileProject(PProject operation, SubPlan plan) {
        final List<CompiledSubPlan> compiledParents = getCompiledFormOfParents(plan);
        final CompiledSubPlan compiledParent = compiledParents.get(0);

        List<PVariable> projectedVariables = new ArrayList<PVariable>(operation.getToVariables());
        // Determinizing projection: try to keep original order (hopefully facilitates node reuse)
        Map<PVariable, Integer> parentPosMapping = compiledParent.getPosMapping();
        Collections.sort(projectedVariables, Comparator.comparing(parentPosMapping::get));

        return doProjectPlan(compiledParent, projectedVariables, true,
                parentTrace -> parentTrace.cloneFor(plan),
                (recipe, parentTrace) -> new PlanningTrace(plan, projectedVariables, recipe, parentTrace),
                (recipe, parentTrace) -> new CompiledSubPlan(plan, projectedVariables, recipe, parentTrace)
        );
    }

    /**
     * Projects a subplan onto the specified variable tuple
     * @param compiledParentPlan the compiled form of the subplan
     * @param targetVariables list of variables to project to
     * @param enforceUniqueness whether distinctness shall be enforced after the projection.
     * Specify false only if directly connecting to a production node.
     * @param reinterpretTraceFactory constructs a reinterpreted trace that simply relabels the compiled parent plan, in case it is sufficient
     * @param intermediateTraceFactory constructs a recipe trace for an intermediate node, given the recipe of the node and its parent trace
     * @param finalTraceFactory constructs a recipe trace for the final resulting node, given the recipe of the node and its parent trace
     * @since 2.1
     */
    <ResultTrace extends RecipeTraceInfo> ResultTrace doProjectPlan(
            final CompiledSubPlan compiledParentPlan,
            final List<PVariable> targetVariables,
            boolean enforceUniqueness,
            Function<CompiledSubPlan, ResultTrace> reinterpretTraceFactory,
            BiFunction<ReteNodeRecipe, RecipeTraceInfo, RecipeTraceInfo> intermediateTraceFactory,
            BiFunction<ReteNodeRecipe, RecipeTraceInfo, ResultTrace> finalTraceFactory)
    {
        if (targetVariables.equals(compiledParentPlan.getVariablesTuple())) // no projection needed
            return reinterpretTraceFactory.apply(compiledParentPlan);

        // otherwise, we need at least a trimmer
        TrimmerRecipe trimmerRecipe = CompilerHelper.makeTrimmerRecipe(compiledParentPlan, targetVariables);

        // do we need to eliminate duplicates?
        SubPlan parentPlan = compiledParentPlan.getSubPlan();
        if (!enforceUniqueness || BuildHelper.areAllVariablesDetermined(
                parentPlan,
                targetVariables,
                queryAnalyzer,
                true))
        {
            // if uniqueness enforcess is unwanted or unneeeded, skip it
            return finalTraceFactory.apply(trimmerRecipe, compiledParentPlan);
        } else {
            // add a uniqueness enforcer
            UniquenessEnforcerRecipe recipe = FACTORY.createUniquenessEnforcerRecipe();
            recipe.getParents().add(trimmerRecipe);

            // temporary solution to support the deprecated option for now
            final boolean deleteAndRederiveEvaluationDep = this.deleteAndRederiveEvaluation || ReteHintOptions.deleteRederiveEvaluation.getValueOrDefault(getHints(parentPlan));

            recipe.setDeleteRederiveEvaluation(deleteAndRederiveEvaluationDep);
            if (deleteAndRederiveEvaluationDep || (this.timelyEvaluation != null)) {
                CompilerHelper.PosetTriplet triplet = CompilerHelper.computePosetInfo(targetVariables, parentPlan.getBody(), metaContext);

                if (triplet.comparator != null) {
                    MonotonicityInfo info = FACTORY.createMonotonicityInfo();
                    info.setCoreMask(triplet.coreMask);
                    info.setPosetMask(triplet.posetMask);
                    info.setPosetComparator(triplet.comparator);
                    recipe.setOptionalMonotonicityInfo(info);
                }
            }

            RecipeTraceInfo trimmerTrace = intermediateTraceFactory.apply(trimmerRecipe, compiledParentPlan);
            return finalTraceFactory.apply(recipe, trimmerTrace);
        }
    }

    /**
     * Projects the final compiled form of a PBody onto the parameter tuple
     * @param compiledBody the compiled form of the body, with all constraints enforced, not yet projected to query parameters
     * @param enforceUniqueness whether distinctness shall be enforced after the projection.
     * Specify false only if directly connecting to a production node.
     * @since 2.1
     */
    RecipeTraceInfo projectBodyFinalToParameters(
            final CompiledSubPlan compiledBody,
            boolean enforceUniqueness)
    {
        final PBody body = compiledBody.getSubPlan().getBody();
        final List<PVariable> parameterList = body.getSymbolicParameterVariables();

        return doProjectPlan(compiledBody, parameterList, enforceUniqueness,
                parentTrace -> parentTrace,
                (recipe, parentTrace) -> new ParameterProjectionTrace(body, recipe, parentTrace),
                (recipe, parentTrace) -> new ParameterProjectionTrace(body, recipe, parentTrace)
        );
    }

    private CompiledSubPlan doCompileStart(PStart operation, SubPlan plan) {
        if (!operation.getAPrioriVariables().isEmpty()) {
            throw new IllegalArgumentException("Input variables unsupported by Rete: " + plan.toShortString());
        }
        final ConstantRecipe recipe = FACTORY.createConstantRecipe();
        recipe.getConstantValues().clear();

        return new CompiledSubPlan(plan, new ArrayList<PVariable>(), recipe);
    }

    private CompiledSubPlan doCompileEnumerate(EnumerablePConstraint constraint, SubPlan plan) {
        final PlanningTrace trimmedTrace = doEnumerateAndDeduplicate(constraint, plan);

        return trimmedTrace.cloneFor(plan);
    }

    private PlanningTrace doEnumerateAndDeduplicate(EnumerablePConstraint constraint, SubPlan plan) {
        final PlanningTrace coreTrace = doEnumerateDispatch(plan, constraint);
        final PlanningTrace trimmedTrace = CompilerHelper.checkAndTrimEqualVariables(plan, coreTrace);
        return trimmedTrace;
    }

    private PlanningTrace doEnumerateDispatch(SubPlan plan, EnumerablePConstraint constraint) {
        if (constraint instanceof RelationEvaluation) {
            return compileEnumerable(plan, (RelationEvaluation) constraint);
        } else if (constraint instanceof BinaryTransitiveClosure) {
            return compileEnumerable(plan, (BinaryTransitiveClosure) constraint);
        } else if (constraint instanceof BinaryReflexiveTransitiveClosure) {
            return compileEnumerable(plan, (BinaryReflexiveTransitiveClosure) constraint);
		} else if (constraint instanceof RepresentativeElectionConstraint) {
			return compileEnumerable(plan, (RepresentativeElectionConstraint) constraint);
        } else if (constraint instanceof ConstantValue) {
            return compileEnumerable(plan, (ConstantValue) constraint);
        } else if (constraint instanceof PositivePatternCall) {
            return compileEnumerable(plan, (PositivePatternCall) constraint);
        } else if (constraint instanceof TypeConstraint) {
            return compileEnumerable(plan, (TypeConstraint) constraint);
        }
        throw new UnsupportedOperationException("Unknown enumerable constraint " + constraint);
    }

    private PlanningTrace compileEnumerable(SubPlan plan, BinaryReflexiveTransitiveClosure constraint) {
        // TODO the implementation would perform better if an inequality check would be used after tcRecipe and
        // uniqueness enforcer be replaced by a transparent node with multiple parents, but such a node is not available
        // in recipe metamodel in VIATRA 2.0

        // Find called query
        final PQuery referredQuery = constraint.getSupplierKey();
        final PlanningTrace callTrace = referQuery(referredQuery, plan, constraint.getVariablesTuple());

        // Calculate irreflexive transitive closure
        final TransitiveClosureRecipe tcRecipe = FACTORY.createTransitiveClosureRecipe();
        tcRecipe.setParent(callTrace.getRecipe());
        final PlanningTrace tcTrace = new PlanningTrace(plan, CompilerHelper.convertVariablesTuple(constraint), tcRecipe, callTrace);

        // Enumerate universe type
        final IInputKey inputKey = constraint.getUniverseType();
        final InputRecipe universeTypeRecipe = RecipesHelper.inputRecipe(inputKey, inputKey.getStringID(), inputKey.getArity());
        final PlanningTrace universeTypeTrace = new PlanningTrace(plan, CompilerHelper.convertVariablesTuple(
                Tuples.staticArityFlatTupleOf(constraint.getVariablesTuple().get(0))), universeTypeRecipe);

        // Calculate reflexive access by duplicating universe type column
        final TrimmerRecipe reflexiveRecipe = FACTORY.createTrimmerRecipe();
        reflexiveRecipe.setMask(RecipesHelper.mask(1, 0, 0));
        reflexiveRecipe.setParent(universeTypeRecipe);
        final PlanningTrace reflexiveTrace = new PlanningTrace(plan, CompilerHelper.convertVariablesTuple(constraint), reflexiveRecipe, universeTypeTrace);

        // Finally, reduce duplicates after a join
        final UniquenessEnforcerRecipe brtcRecipe = FACTORY.createUniquenessEnforcerRecipe();
        brtcRecipe.getParents().add(tcRecipe);
        brtcRecipe.getParents().add(reflexiveRecipe);

        return new PlanningTrace(plan, CompilerHelper.convertVariablesTuple(constraint), brtcRecipe, reflexiveTrace, tcTrace);
    }

	private PlanningTrace compileEnumerable(SubPlan plan, RepresentativeElectionConstraint constraint) {
		var referredQuery = constraint.getSupplierKey();
		var callTrace = referQuery(referredQuery, plan, constraint.getVariablesTuple());
		var recipe = FACTORY.createRepresentativeElectionRecipe();
		recipe.setParent(callTrace.getRecipe());
		recipe.setConnectivity(constraint.getConnectivity());
		return new PlanningTrace(plan, CompilerHelper.convertVariablesTuple(constraint), recipe, callTrace);
	}

    private PlanningTrace compileEnumerable(SubPlan plan, BinaryTransitiveClosure constraint) {
        final PQuery referredQuery = constraint.getSupplierKey();
        final PlanningTrace callTrace = referQuery(referredQuery, plan, constraint.getVariablesTuple());

        final TransitiveClosureRecipe recipe = FACTORY.createTransitiveClosureRecipe();
        recipe.setParent(callTrace.getRecipe());

        return new PlanningTrace(plan, CompilerHelper.convertVariablesTuple(constraint), recipe, callTrace);
    }

    private PlanningTrace compileEnumerable(SubPlan plan, RelationEvaluation constraint) {
        final List<ReteNodeRecipe> parentRecipes = new ArrayList<ReteNodeRecipe>();
        final List<RecipeTraceInfo> parentTraceInfos = new ArrayList<RecipeTraceInfo>();
        for (final PQuery inputQuery : constraint.getReferredQueries()) {
            final CompiledQuery compiledQuery = getCompiledForm(inputQuery);
            parentRecipes.add(compiledQuery.getRecipe());
            parentTraceInfos.add(compiledQuery);
        }
        final RelationEvaluationRecipe recipe = FACTORY.createRelationEvaluationRecipe();
        recipe.getParents().addAll(parentRecipes);
        recipe.setEvaluator(RecipesHelper.expressionDefinition(constraint.getEvaluator()));
        return new PlanningTrace(plan, CompilerHelper.convertVariablesTuple(constraint), recipe, parentTraceInfos);
    }

    private PlanningTrace compileEnumerable(SubPlan plan, PositivePatternCall constraint) {
        final PQuery referredQuery = constraint.getReferredQuery();
        return referQuery(referredQuery, plan, constraint.getVariablesTuple());
    }

    private PlanningTrace compileEnumerable(SubPlan plan, TypeConstraint constraint) {
        final IInputKey inputKey = constraint.getSupplierKey();
        final InputRecipe recipe = RecipesHelper.inputRecipe(inputKey, inputKey.getStringID(), inputKey.getArity());
        return new PlanningTrace(plan, CompilerHelper.convertVariablesTuple(constraint), recipe);
    }

    private PlanningTrace compileEnumerable(SubPlan plan, ConstantValue constraint) {
        final ConstantRecipe recipe = FACTORY.createConstantRecipe();
        recipe.getConstantValues().add(constraint.getSupplierKey());
        return new PlanningTrace(plan, CompilerHelper.convertVariablesTuple(constraint), recipe);
    }

    // TODO handle recursion
    private PlanningTrace referQuery(PQuery query, SubPlan plan, Tuple actualParametersTuple) {
        RecipeTraceInfo referredQueryTrace = originalTraceOfReferredQuery(query);
        return new PlanningTrace(plan, CompilerHelper.convertVariablesTuple(actualParametersTuple),
                referredQueryTrace.getRecipe(), referredQueryTrace.getParentRecipeTracesForCloning());
    }

    private RecipeTraceInfo originalTraceOfReferredQuery(PQuery query) {
        // eliminate superfluous production node?
        if (PVisibility.EMBEDDED == query.getVisibility()) { // currently inline patterns only
            Set<PBody> rewrittenBodies = normalizer.rewrite(query).getBodies();
            if (1 == rewrittenBodies.size()) { // non-disjunctive
                // TODO in the future, check if non-recursive - (not currently permitted)

                PBody pBody = rewrittenBodies.iterator().next();
                SubPlan bodyFinalPlan = getPlan(pBody);

                // skip over any projections at the end
                bodyFinalPlan = BuildHelper.eliminateTrailingProjections(bodyFinalPlan);

                // TODO checkAndTrimEqualVariables may introduce superfluous trim,
                // but whatever (no uniqueness enforcer needed)

                // compile body
                final CompiledSubPlan compiledBody = getCompiledForm(bodyFinalPlan);

                // project to parameter list, add uniqueness enforcer if necessary
                return projectBodyFinalToParameters(compiledBody, true /* ensure uniqueness, as no production node is used */);
            }
        }

        // otherwise, regular reference to recipe realizing the query
        return getCompiledForm(query);
    }

    protected List<CompiledSubPlan> getCompiledFormOfParents(SubPlan plan) {
        List<CompiledSubPlan> results = new ArrayList<CompiledSubPlan>();
        for (SubPlan parentPlan : plan.getParentPlans()) {
            results.add(getCompiledForm(parentPlan));
        }
        return results;
    }

    /**
     * Returns an unmodifiable view of currently cached compiled queries.
     */
    public Map<PQuery, CompiledQuery> getCachedCompiledQueries() {
        return Collections.unmodifiableMap(queryCompilerCache);
    }

    /**
     * Returns an unmodifiable view of currently cached query plans.
     */
    public Map<PBody, SubPlan> getCachedQueryPlans() {
        return Collections.unmodifiableMap(plannerCache);
    }

    private QueryEvaluationHint getHints(SubPlan plan) {
        return getHints(plan.getBody().getPattern());
    }

    private QueryEvaluationHint getHints(PQuery pattern) {
        return hintProvider.getQueryEvaluationHint(pattern);
    }
}
