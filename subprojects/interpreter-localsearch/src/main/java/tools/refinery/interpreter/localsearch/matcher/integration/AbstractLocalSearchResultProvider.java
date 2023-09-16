/*******************************************************************************
 * Copyright (c) 2010-2017, Zoltan Ujhelyi, IncQuery Labs Ltd.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.interpreter.localsearch.matcher.integration;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import tools.refinery.interpreter.localsearch.exceptions.LocalSearchException;
import tools.refinery.interpreter.localsearch.planner.compiler.IOperationCompiler;
import tools.refinery.interpreter.localsearch.matcher.CallWithAdornment;
import tools.refinery.interpreter.localsearch.matcher.ISearchContext;
import tools.refinery.interpreter.localsearch.matcher.LocalSearchMatcher;
import tools.refinery.interpreter.localsearch.matcher.MatcherReference;
import tools.refinery.interpreter.localsearch.plan.IPlanDescriptor;
import tools.refinery.interpreter.localsearch.plan.IPlanProvider;
import tools.refinery.interpreter.localsearch.plan.SearchPlan;
import tools.refinery.interpreter.localsearch.plan.SearchPlanForBody;
import tools.refinery.interpreter.matchers.InterpreterRuntimeException;
import tools.refinery.interpreter.matchers.backend.IMatcherCapability;
import tools.refinery.interpreter.matchers.backend.IQueryBackend;
import tools.refinery.interpreter.matchers.backend.IQueryResultProvider;
import tools.refinery.interpreter.matchers.backend.IUpdateable;
import tools.refinery.interpreter.matchers.backend.QueryEvaluationHint;
import tools.refinery.interpreter.matchers.backend.QueryHintOption;
import tools.refinery.interpreter.matchers.backend.ResultProviderRequestor;
import tools.refinery.interpreter.matchers.context.IInputKey;
import tools.refinery.interpreter.matchers.context.IQueryBackendContext;
import tools.refinery.interpreter.matchers.context.IQueryRuntimeContext;
import tools.refinery.interpreter.matchers.context.IndexingService;
import tools.refinery.interpreter.matchers.planning.QueryProcessingException;
import tools.refinery.interpreter.matchers.planning.helpers.FunctionalDependencyHelper;
import tools.refinery.interpreter.matchers.psystem.IQueryReference;
import tools.refinery.interpreter.matchers.psystem.PBody;
import tools.refinery.interpreter.matchers.psystem.basicenumerables.PositivePatternCall;
import tools.refinery.interpreter.matchers.psystem.queries.PParameter;
import tools.refinery.interpreter.matchers.psystem.queries.PQueries;
import tools.refinery.interpreter.matchers.psystem.queries.PQuery;
import tools.refinery.interpreter.matchers.psystem.rewriters.IFlattenCallPredicate;
import tools.refinery.interpreter.matchers.tuple.ITuple;
import tools.refinery.interpreter.matchers.tuple.Tuple;
import tools.refinery.interpreter.matchers.tuple.TupleMask;
import tools.refinery.interpreter.matchers.util.Accuracy;

/**
 * @author Zoltan Ujhelyi
 * @since 1.7
 *
 */
public abstract class AbstractLocalSearchResultProvider implements IQueryResultProvider {

    protected final LocalSearchBackend backend;
    protected final IQueryBackendContext backendContext;
    protected final IQueryRuntimeContext runtimeContext;
    protected final PQuery query;
    protected final QueryEvaluationHint userHints;
    protected final Map<PQuery, LocalSearchHints> hintCache = new HashMap<>();
    protected final IPlanProvider planProvider;
    private static final String PLAN_CACHE_KEY = AbstractLocalSearchResultProvider.class.getName() + "#planCache";
    private final Map<MatcherReference, IPlanDescriptor> planCache;
    protected final ISearchContext searchContext;
    /**
     * @since 2.1
     */
    protected ResultProviderRequestor resultProviderRequestor;

    /**
     * @since 1.5
     */
    @SuppressWarnings({ "unchecked"})
    public AbstractLocalSearchResultProvider(LocalSearchBackend backend, IQueryBackendContext context, PQuery query,
            IPlanProvider planProvider, QueryEvaluationHint userHints) {
        this.backend = backend;
        this.backendContext = context;
        this.query = query;

        this.planProvider = planProvider;
        this.userHints = userHints;
        this.runtimeContext = context.getRuntimeContext();
        this.resultProviderRequestor = backend.getResultProviderRequestor(query, userHints);
        this.searchContext = new ISearchContext.SearchContext(backendContext, backend.getCache(), resultProviderRequestor);
        this.planCache = backend.getCache().getValue(PLAN_CACHE_KEY, Map.class, HashMap::new);
    }

    protected abstract IOperationCompiler getOperationCompiler(IQueryBackendContext backendContext, LocalSearchHints configuration);

    private IQueryRuntimeContext getRuntimeContext() {
        return backend.getRuntimeContext();
    }

    private LocalSearchMatcher createMatcher(IPlanDescriptor plan, final ISearchContext searchContext) {
        List<SearchPlan> executors = plan.getPlan().stream()
                .map(input -> new SearchPlan(input.getBody(), input.getCompiledOperations(), input.calculateParameterMask(),
                        input.getVariableKeys()))
                .collect(Collectors.toList());
        return new LocalSearchMatcher(searchContext, plan, executors);
    }

    private IPlanDescriptor getOrCreatePlan(MatcherReference key, IQueryBackendContext backendContext, IOperationCompiler compiler, LocalSearchHints configuration, IPlanProvider planProvider) {
        if (planCache.containsKey(key)){
            return planCache.get(key);
        } else {
            IPlanDescriptor plan = planProvider.getPlan(backendContext, compiler,
                    resultProviderRequestor, configuration, key);
            planCache.put(key, plan);
            return plan;
        }
    }

    private IPlanDescriptor getOrCreatePlan(MatcherReference key, IPlanProvider planProvider) {
        if (planCache.containsKey(key)){
            return planCache.get(key);
        } else {
            LocalSearchHints configuration = overrideDefaultHints(key.getQuery());
            IOperationCompiler compiler = getOperationCompiler(backendContext, configuration);
            IPlanDescriptor plan = planProvider.getPlan(backendContext, compiler,
                    resultProviderRequestor, configuration, key);
            planCache.put(key, plan);
            return plan;
        }
    }

    private LocalSearchHints overrideDefaultHints(PQuery pQuery) {
        if (hintCache.containsKey(pQuery)) {
            return hintCache.get(pQuery);
        } else {
            LocalSearchHints hint = LocalSearchHints.getDefaultOverriddenBy(
                    computeOverridingHints(pQuery));
            hintCache.put(pQuery, hint);
            return hint;
        }
    }

    /**
     * Combine with {@link QueryHintOption#getValueOrDefault(QueryEvaluationHint)} to access
     * hint settings not covered by {@link LocalSearchHints}
     */
    private QueryEvaluationHint computeOverridingHints(PQuery pQuery) {
        return backendContext.getHintProvider().getQueryEvaluationHint(pQuery).overrideBy(userHints);
    }

    /**
     * Prepare this result provider. This phase is separated from the constructor to allow the backend to cache its instance before
     * requesting preparation for its dependencies.
     * @since 1.5
     */
    public void prepare() {
        try {
            runtimeContext.coalesceTraversals(() -> {
                LocalSearchHints configuration = overrideDefaultHints(query);
                if (configuration.isUseBase()) {
                    indexInitializationBeforePlanning();
                }
                prepareDirectDependencies();
                runtimeContext.executeAfterTraversal(AbstractLocalSearchResultProvider.this::preparePlansForExpectedAdornments);
                return null;
            });
        } catch (InvocationTargetException e) {
            throw new QueryProcessingException("Error while building required indexes: {1}", new String[]{e.getTargetException().getMessage()}, "Error while building required indexes.", query, e);
        }
    }

    protected void preparePlansForExpectedAdornments() {
        // Plan for possible adornments
        for (Set<PParameter> adornment : overrideDefaultHints(query).getAdornmentProvider().getAdornments(query)) {
            MatcherReference reference = new MatcherReference(query, adornment, userHints);
            LocalSearchHints configuration = overrideDefaultHints(query);
            IOperationCompiler compiler = getOperationCompiler(backendContext, configuration);
            IPlanDescriptor plan = getOrCreatePlan(reference, backendContext, compiler, configuration, planProvider);
            // Index keys
            try {
                if (configuration.isUseBase()) {
                    indexKeys(plan.getIteratedKeys());
                }
            } catch (InvocationTargetException e) {
                throw new QueryProcessingException(e.getMessage(), null, e.getMessage(), query, e);
            }
            //Prepare dependencies
            for(SearchPlanForBody body: plan.getPlan()){
                for(CallWithAdornment dependency : body.getDependencies()){
                    searchContext.getMatcher(dependency);
                }
            }
        }
    }

    protected void prepareDirectDependencies() {
        // Do not prepare for any adornment at this point
        IAdornmentProvider adornmentProvider = input -> Collections.emptySet();
        QueryEvaluationHint adornmentHint = IAdornmentProvider.toHint(adornmentProvider);

        for(IQueryReference call : getDirectDependencies()){
            resultProviderRequestor.requestResultProvider(call, adornmentHint);
        }
    }

    /**
     * This method is called before planning start to allow indexing. It is important to note that this method is called
     * inside a coalesceTraversals block, meaning (1) it is safe to add multiple registration requests as necessary, but
     * (2) no value or statistics is available from the index.
     *
     * @throws InterpreterRuntimeException
     */
    protected void indexInitializationBeforePlanning() {
        // By default, no indexing is necessary
    }

    /**
     * Collects and indexes all types _directly_ referred by the PQuery {@link #query}. Types indirect
     * @param requiredIndexingServices
     */
    protected void indexReferredTypesOfQuery(PQuery query, IndexingService requiredIndexingServices) {
        PQueries.directlyRequiredTypesOfQuery(query, true /*only enumerables are considered for indexing */).forEach(
                inputKey -> runtimeContext.ensureIndexed(inputKey, requiredIndexingServices)
        );
    }

    private Set<IQueryReference> getDirectDependencies() {
        IFlattenCallPredicate flattenPredicate = overrideDefaultHints(query).getFlattenCallPredicate();
        Queue<PQuery> queue = new LinkedList<>();
        Set<PQuery> visited = new HashSet<>();
        Set<IQueryReference> result = new HashSet<>();
        queue.add(query);

        while(!queue.isEmpty()){
            PQuery next = queue.poll();
            visited.add(next);
            for(PBody body : next.getDisjunctBodies().getBodies()){
                for (IQueryReference call : body.getConstraintsOfType(IQueryReference.class)) {
                    if (call instanceof PositivePatternCall &&
                            flattenPredicate.shouldFlatten((PositivePatternCall) call))
                    {
                        PQuery dep = ((PositivePatternCall) call).getReferredQuery();
                        if (!visited.contains(dep)){
                            queue.add(dep);
                        }
                    } else {
                        result.add(call);
                    }
                }
            }
        }
        return result;
    }

    private LocalSearchMatcher initializeMatcher(Object[] parameters) {
        return newLocalSearchMatcher(parameters);
    }

    private LocalSearchMatcher initializeMatcher(TupleMask parameterSeedMask) {
        return newLocalSearchMatcher(parameterSeedMask.transformUnique(query.getParameters()));

    }


    /**
     * @throws InterpreterRuntimeException
     */
    public LocalSearchMatcher newLocalSearchMatcher(ITuple parameters) {
        final Set<PParameter> adornment = new HashSet<>();
        for (int i = 0; i < parameters.getSize(); i++) {
            if (parameters.get(i) != null) {
                adornment.add(query.getParameters().get(i));
            }
        }

        return newLocalSearchMatcher(adornment);
    }

    /**
     * @throws InterpreterRuntimeException
     */
    public LocalSearchMatcher newLocalSearchMatcher(Object[] parameters) {
        final Set<PParameter> adornment = new HashSet<>();
        for (int i = 0; i < parameters.length; i++) {
            if (parameters[i] != null) {
                adornment.add(query.getParameters().get(i));
            }
        }

        return newLocalSearchMatcher(adornment);
    }

    private LocalSearchMatcher newLocalSearchMatcher(final Set<PParameter> adornment) {
        final MatcherReference reference = new MatcherReference(query, adornment, userHints);

        IPlanDescriptor plan = getOrCreatePlan(reference, planProvider);
        if (overrideDefaultHints(reference.getQuery()).isUseBase()){
            try {
                indexKeys(plan.getIteratedKeys());
            } catch (InvocationTargetException e) {
                throw new LocalSearchException("Could not index keys", e);
            }
        }

        LocalSearchMatcher matcher = createMatcher(plan, searchContext);
        matcher.addAdapters(backend.getAdapters());
        return matcher;
    }

    private void indexKeys(final Iterable<IInputKey> keys) throws InvocationTargetException {
        final IQueryRuntimeContext qrc = getRuntimeContext();
        qrc.coalesceTraversals(new Callable<Void>() {

            @Override
            public Void call() throws Exception {
                for(IInputKey key : keys){
                    if (key.isEnumerable()) {
                        qrc.ensureIndexed(key, IndexingService.INSTANCES);
                    }
                }
                return null;
            }
        });
    }

    @Override
    public boolean hasMatch(Object[] parameters) {
        final LocalSearchMatcher matcher = initializeMatcher(parameters);
        return matcher.streamMatches(parameters).findAny().isPresent();
    }

    @Override
    public boolean hasMatch(TupleMask parameterSeedMask, ITuple parameters) {
        final LocalSearchMatcher matcher = initializeMatcher(parameterSeedMask);
        return matcher.streamMatches(parameterSeedMask, parameters).findAny().isPresent();
    }

    @Override
    public Optional<Tuple> getOneArbitraryMatch(Object[] parameters) {
        final LocalSearchMatcher matcher = initializeMatcher(parameters);
        return matcher.streamMatches(parameters).findAny();
    }

    @Override
    public Optional<Tuple> getOneArbitraryMatch(TupleMask parameterSeedMask, ITuple parameters) {
        final LocalSearchMatcher matcher = initializeMatcher(parameterSeedMask);
        return matcher.streamMatches(parameterSeedMask, parameters).findAny();
    }

    @Override
    public int countMatches(Object[] parameters) {
        final LocalSearchMatcher matcher = initializeMatcher(parameters);
        // Count returns long; casting to int - in case of integer overflow casting will throw the exception
        return (int) matcher.streamMatches(parameters).count();
    }

    @Override
    public int countMatches(TupleMask parameterSeedMask, ITuple parameters) {
        final LocalSearchMatcher matcher = initializeMatcher(parameterSeedMask);
        // Count returns long; casting to int - in case of integer overflow casting will throw the exception
        return (int) matcher.streamMatches(parameterSeedMask, parameters).count();
    }

    private static final double ESTIMATE_CEILING = Long.MAX_VALUE / 16.0;

    @Override
    public Optional<Long> estimateCardinality(TupleMask groupMask, Accuracy requiredAccuracy) {
        if (Accuracy.BEST_UPPER_BOUND.atLeastAsPreciseAs(requiredAccuracy)) { // approximate using parameter types
            final List<PParameter> parameters = query.getParameters();
            final Map<Set<Integer>, Set<Integer>> dependencies = backendContext.getQueryAnalyzer()
                    .getProjectedFunctionalDependencies(query, false);

            List<Integer> projectionIndices = groupMask.getIndicesAsList();

            return estimateParameterCombinations(requiredAccuracy, parameters, dependencies,
                    projectionIndices,
                    Collections.emptySet() /* No parameters with fixed value */).map(Double::longValue);
        }
        else return Optional.empty();
    }

    @Override
    public Optional<Double> estimateAverageBucketSize(TupleMask groupMask, Accuracy requiredAccuracy) {
        if (Accuracy.BEST_UPPER_BOUND.atLeastAsPreciseAs(requiredAccuracy)) { // approximate using parameter types
            final List<PParameter> parameters = query.getParameters();
            final Map<Set<Integer>, Set<Integer>> dependencies = backendContext.getQueryAnalyzer()
                    .getProjectedFunctionalDependencies(query, false);

            // all parameters used for the estimation - determinized order
            final List<Integer> allParameterIndices =
                    IntStream.range(0, parameters.size()).boxed().collect(Collectors.toList());

            // some free parameters are functionally determined by bound parameters
            final Set<Integer> boundOrImplied = FunctionalDependencyHelper.closureOf(groupMask.getIndicesAsList(),
                    dependencies);

            return estimateParameterCombinations(requiredAccuracy, parameters, dependencies,
                    allParameterIndices,
                    boundOrImplied);
        }
        else return Optional.empty();
    }

    /**
     * @since 2.1
     * @noreference This method is not intended to be referenced by clients.
     */
    public double estimateCost(TupleMask inputBindingMask) {
        // TODO this is currently an abstract cost, not really a branching factor

        HashSet<PParameter> adornment = new HashSet<>(inputBindingMask.transform(query.getParameters()));
        final MatcherReference reference = new MatcherReference(query, adornment, userHints);
        IPlanDescriptor plan = getOrCreatePlan(reference, planProvider);

        return plan.getPlan().stream().mapToDouble(SearchPlanForBody::getCost).sum();
    }

    /**
     * Approximates using parameter types
     */
    private Optional<Double> estimateParameterCombinations(
            Accuracy requiredAccuracy,
            final List<PParameter> parameters,
            final Map<Set<Integer>, Set<Integer>> functionalDependencies,
            final Collection<Integer> parameterIndicesToEstimate,
            final Set<Integer> otherDeterminingIndices)
    {
        // keep order deterministic
        LinkedHashSet<Integer> freeParameterIndices = new LinkedHashSet<>(parameterIndicesToEstimate);

        // determining indices are bound
        freeParameterIndices.removeAll(otherDeterminingIndices);

        // some free parameters are functionally determined by other free parameters
        for (Integer candidateForRemoval : new ArrayList<>(freeParameterIndices)) {
            List<Integer> others = Stream.concat(
                    otherDeterminingIndices.stream(),
                    freeParameterIndices.stream().filter(index -> !Objects.equals(index, candidateForRemoval))
            ).collect(Collectors.toList());
            Set<Integer> othersClosure = FunctionalDependencyHelper.closureOf(others, functionalDependencies);
            if (othersClosure.contains(candidateForRemoval)) {
                // other parameters functionally determine this mone, does not count towards estimate
                freeParameterIndices.remove(candidateForRemoval);
            }
        }


        Optional<Double> result = Optional.of(1.0);
        // TODO this is currently works with declared types only. For better results, information from
        // the Type inferrer should be included in the PSystem
        for (int i = 0; (i < parameters.size()); i++) {
            final IInputKey type = parameters.get(i).getDeclaredUnaryType();
            if (freeParameterIndices.contains(i) && type != null) {
                result = result.flatMap(accumulator ->
                runtimeContext.estimateCardinality(type, TupleMask.identity(1), requiredAccuracy).map(multiplier ->
                    Math.min(accumulator * multiplier, ESTIMATE_CEILING /* avoid overflow */)
                ));
            }
        }
        // TODO better approximate cardinality based on plan, branching factors, etc.
        return result;
    }


    @Override
    public Stream<Tuple> getAllMatches(Object[] parameters) {
        final LocalSearchMatcher matcher = initializeMatcher(parameters);
        return matcher.streamMatches(parameters);
    }

    @Override
    public Stream<Tuple> getAllMatches(TupleMask parameterSeedMask, ITuple parameters) {
        final LocalSearchMatcher matcher = initializeMatcher(parameterSeedMask);
        return matcher.streamMatches(parameterSeedMask, parameters);
    }

    @Override
    public IQueryBackend getQueryBackend() {
        return backend;
    }

    @Override
    public void addUpdateListener(IUpdateable listener, Object listenerTag, boolean fireNow) {
        // throw new UnsupportedOperationException(UPDATE_LISTENER_NOT_SUPPORTED);
    }

    @Override
    public void removeUpdateListener(Object listenerTag) {
        // throw new UnsupportedOperationException(UPDATE_LISTENER_NOT_SUPPORTED);
    }

    /**
     * @since 1.4
     */
    public IMatcherCapability getCapabilites() {
        LocalSearchHints configuration = overrideDefaultHints(query);
        return configuration;
    }

    /**
     * Forgets all stored plans in this result provider. If no plans are stored, nothing happens.
     *
     * @since 2.0
     * @noreference This method is not intended to be referenced by clients; it should only used by {@link LocalSearchBackend}.
     */
    public void forgetAllPlans() {
        planCache.clear();
    }

    /**
     * Returns a search plan for a given adornment if exists
     *
     * @return a search plan for the pattern with the given adornment, or null if none exists
     * @since 2.0
     * @noreference This method is not intended to be referenced by clients; it should only used by {@link LocalSearchBackend}.
     */
    public IPlanDescriptor getSearchPlan(Set<PParameter> adornment) {
        return planCache.get(new MatcherReference(query, adornment));
    }
}
