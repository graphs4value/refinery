/*******************************************************************************
 * Copyright (c) 2010-2016, Grill Balázs, IncQuery Labs Ltd.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.interpreter.localsearch.matcher.integration;

import tools.refinery.interpreter.localsearch.planner.cost.ICostFunction;
import tools.refinery.interpreter.localsearch.planner.cost.impl.IndexerBasedConstraintCostFunction;
import tools.refinery.interpreter.localsearch.planner.cost.impl.StatisticsBasedConstraintCostFunction;
import tools.refinery.interpreter.matchers.backend.*;
import tools.refinery.interpreter.matchers.psystem.rewriters.IFlattenCallPredicate;
import tools.refinery.interpreter.matchers.psystem.rewriters.IRewriterTraceCollector;
import tools.refinery.interpreter.matchers.psystem.rewriters.NopTraceCollector;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static tools.refinery.interpreter.localsearch.matcher.integration.LocalSearchHintOptions.*;
import static tools.refinery.interpreter.matchers.backend.CommonQueryHintOptions.normalizationTraceCollector;

/**
 * Type safe builder and extractor for Local search specific hints
 *
 * @author Grill Balázs
 * @since 1.4
 *
 */
public final class LocalSearchHints implements IMatcherCapability {

    private Boolean useBase = null;

    private Integer rowCount = null;

    private ICostFunction costFunction = null;

    private IFlattenCallPredicate flattenCallPredicate = null;

    private ICallDelegationStrategy callDelegationStrategy = null;

    private IAdornmentProvider adornmentProvider = null;

    private IRewriterTraceCollector traceCollector = NopTraceCollector.INSTANCE;

    private IQueryBackendFactory backendFactory = null;

    private LocalSearchHints() {}

    /**
     * Return the default settings overridden by the given hints
     */
    public static LocalSearchHints getDefaultOverriddenBy(QueryEvaluationHint overridingHint){
        return parse(getDefault().build(overridingHint));
    }

    /**
     * Default settings which are considered the most safe, providing a reasonable performance for most of the cases. Assumes the availability of the base indexer.
     */
    public static LocalSearchHints getDefault(){
        return getDefaultGeneric();
    }

    /**
     * Initializes the generic (not EMF specific) search backend with the default settings
     * @since 1.7
     */
    public static LocalSearchHints getDefaultGeneric(){
        LocalSearchHints result = new LocalSearchHints();
        result.useBase = true; // Should be unused; but a false value might cause surprises as an engine-default hint
        result.rowCount = 4;
        result.costFunction = new IndexerBasedConstraintCostFunction(StatisticsBasedConstraintCostFunction.INVERSE_NAVIGATION_PENALTY_GENERIC);
        result.flattenCallPredicate = FLATTEN_CALL_PREDICATE.getDefaultValue();
        result.callDelegationStrategy = ICallDelegationStrategy.FULL_BACKEND_ADHESION;
        result.adornmentProvider = new LazyPlanningAdornments();
        result.backendFactory = LocalSearchGenericBackendFactory.INSTANCE;
        return result;
    }

    /**
     * Initializes the default search backend with hybrid-enabled settings
     * @since 2.1
     */
    public static LocalSearchHints getDefaultHybrid(){
        LocalSearchHints result = getDefault();
        result.callDelegationStrategy = ICallDelegationStrategy.PARTIAL_BACKEND_ADHESION;
        result.flattenCallPredicate = new IFlattenCallPredicate.And(
                new DontFlattenIncrementalPredicate(), new DontFlattenDisjunctive());
        return result;
    }

    /**
     * Initializes the generic (not EMF specific) search backend with hybrid-enabled settings
     * @since 2.1
     */
    public static LocalSearchHints getDefaultGenericHybrid(){
        LocalSearchHints result = getDefaultGeneric();
        result.callDelegationStrategy = ICallDelegationStrategy.PARTIAL_BACKEND_ADHESION;
        result.flattenCallPredicate = new IFlattenCallPredicate.And(
                new DontFlattenIncrementalPredicate(), new DontFlattenDisjunctive());
        return result;
    }

    public static LocalSearchHints parse(QueryEvaluationHint hint){
        LocalSearchHints result = new LocalSearchHints();

        result.useBase = USE_BASE_INDEX.getValueOrNull(hint);
        result.rowCount = PLANNER_TABLE_ROW_COUNT.getValueOrNull(hint);
        result.flattenCallPredicate = FLATTEN_CALL_PREDICATE.getValueOrNull(hint);
        result.callDelegationStrategy = CALL_DELEGATION_STRATEGY.getValueOrNull(hint);
        result.costFunction = PLANNER_COST_FUNCTION.getValueOrNull(hint);
        result.adornmentProvider = ADORNMENT_PROVIDER.getValueOrNull(hint);
        result.traceCollector = normalizationTraceCollector.getValueOrDefault(hint);

        return result;
    }


    private Map<QueryHintOption<?>, Object> calculateHintMap() {
        Map<QueryHintOption<?>, Object> map = new HashMap<>();
        if (useBase != null){
            USE_BASE_INDEX.insertOverridingValue(map, useBase);
        }
        if (rowCount != null){
            PLANNER_TABLE_ROW_COUNT.insertOverridingValue(map, rowCount);
        }
        if (costFunction != null){
            PLANNER_COST_FUNCTION.insertOverridingValue(map, costFunction);
        }
        if (flattenCallPredicate != null){
            FLATTEN_CALL_PREDICATE.insertOverridingValue(map, flattenCallPredicate);
        }
        if (callDelegationStrategy != null){
            CALL_DELEGATION_STRATEGY.insertOverridingValue(map, callDelegationStrategy);
        }
        if (adornmentProvider != null){
            ADORNMENT_PROVIDER.insertOverridingValue(map, adornmentProvider);
        }
        if (traceCollector != null){
            normalizationTraceCollector.insertOverridingValue(map, traceCollector);
        }
        return map;
    }

    public QueryEvaluationHint build(){
        Map<QueryHintOption<?>, Object> map = calculateHintMap();
        return new QueryEvaluationHint(map, backendFactory);
    }

    /**
     * @since 1.7
     */
    public QueryEvaluationHint build(QueryEvaluationHint overridingHint) {
        if (overridingHint == null)
            return build();

        IQueryBackendFactory factory = (overridingHint.getQueryBackendFactory() == null)
                ? this.backendFactory
                : overridingHint.getQueryBackendFactory();

        Map<QueryHintOption<?>, Object> hints = calculateHintMap();
        if (overridingHint.getBackendHintSettings() != null) {
            hints.putAll(overridingHint.getBackendHintSettings());
        }

        return new QueryEvaluationHint(hints, factory);
    }

    public boolean isUseBase() {
        return useBase;
    }

    public ICostFunction getCostFunction() {
        return costFunction;
    }

    public IFlattenCallPredicate getFlattenCallPredicate() {
        return flattenCallPredicate;
    }

    /**
     * @since 2.1
     */
    public ICallDelegationStrategy getCallDelegationStrategy() {
        return callDelegationStrategy;
    }

    public Integer getRowCount() {
        return rowCount;
    }

    /**
     * @since 1.5
     */
    public IAdornmentProvider getAdornmentProvider() {
        return adornmentProvider;
    }

    /**
     * @since 1.6
     */
    public IRewriterTraceCollector getTraceCollector() {
        return traceCollector == null ? normalizationTraceCollector.getDefaultValue() : traceCollector;
    }

    public LocalSearchHints setUseBase(boolean useBase) {
        this.useBase = useBase;
        return this;
    }

    public LocalSearchHints setRowCount(int rowCount) {
        this.rowCount = rowCount;
        return this;
    }

    public LocalSearchHints setCostFunction(ICostFunction costFunction) {
        this.costFunction = costFunction;
        return this;
    }

    public LocalSearchHints setFlattenCallPredicate(IFlattenCallPredicate flattenCallPredicate) {
        this.flattenCallPredicate = flattenCallPredicate;
        return this;
    }


    /**
     * @since 2.1
     */
    public LocalSearchHints setCallDelegationStrategy(ICallDelegationStrategy callDelegationStrategy) {
        this.callDelegationStrategy = callDelegationStrategy;
        return this;
    }

    /**
     * @since 1.6
     */
    public LocalSearchHints setTraceCollector(IRewriterTraceCollector traceCollector) {
        this.traceCollector = traceCollector;
        return this;
    }

    /**
     * @since 1.5
     */
    public LocalSearchHints setAdornmentProvider(IAdornmentProvider adornmentProvider) {
        this.adornmentProvider = adornmentProvider;
        return this;
    }

    public static LocalSearchHints customizeUseBase(boolean useBase){
        return new LocalSearchHints().setUseBase(useBase);
    }

    public static LocalSearchHints customizeRowCount(int rowCount){
        return new LocalSearchHints().setRowCount(rowCount);
    }

    public static LocalSearchHints customizeCostFunction(ICostFunction costFunction){
        return new LocalSearchHints().setCostFunction(costFunction);
    }

    public static LocalSearchHints customizeFlattenCallPredicate(IFlattenCallPredicate predicate){
        return new LocalSearchHints().setFlattenCallPredicate(predicate);
    }

    /**
     * @since 2.1
     */
    public static LocalSearchHints customizeCallDelegationStrategy(ICallDelegationStrategy strategy){
        return new LocalSearchHints().setCallDelegationStrategy(strategy);
    }

    /**
     * @since 1.5
     */
    public static LocalSearchHints customizeAdornmentProvider(IAdornmentProvider adornmentProvider){
        return new LocalSearchHints().setAdornmentProvider(adornmentProvider);
    }

    /**
     * @since 1.6
     */
    public static LocalSearchHints customizeTraceCollector(IRewriterTraceCollector traceCollector){
        return new LocalSearchHints().setTraceCollector(traceCollector);
    }

    @Override
    public boolean canBeSubstitute(IMatcherCapability capability) {
        if (capability instanceof LocalSearchHints){
            LocalSearchHints other = (LocalSearchHints)capability;
            /*
             * We allow substitution of matchers if their functionally relevant settings are equal.
             */
            return Objects.equals(other.useBase, useBase);
        }
        /*
         * For any other cases (e.g. for Rete), we cannot assume
         * that matchers created by LS are functionally equivalent.
         */
        return false;
    }
}
