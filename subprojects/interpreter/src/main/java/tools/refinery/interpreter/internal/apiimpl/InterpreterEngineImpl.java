/*******************************************************************************
 * Copyright (c) 2004-2010 Gabor Bergmann and Daniel Varro
 * Copyright (c) 2023 The Refinery Authors <https://refinery.tools>
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package tools.refinery.interpreter.internal.apiimpl;

import org.apache.log4j.Logger;
import tools.refinery.interpreter.api.*;
import tools.refinery.interpreter.api.impl.BaseMatcher;
import tools.refinery.interpreter.api.scope.IBaseIndex;
import tools.refinery.interpreter.api.scope.IEngineContext;
import tools.refinery.interpreter.api.scope.IIndexingErrorListener;
import tools.refinery.interpreter.api.scope.QueryScope;
import tools.refinery.interpreter.exception.InterpreterException;
import tools.refinery.interpreter.internal.engine.LifecycleProvider;
import tools.refinery.interpreter.internal.engine.ModelUpdateProvider;
import tools.refinery.interpreter.matchers.InterpreterRuntimeException;
import tools.refinery.interpreter.matchers.backend.*;
import tools.refinery.interpreter.matchers.context.IQueryBackendContext;
import tools.refinery.interpreter.matchers.context.IQueryCacheContext;
import tools.refinery.interpreter.matchers.context.IQueryResultProviderAccess;
import tools.refinery.interpreter.matchers.context.IQueryRuntimeContext;
import tools.refinery.interpreter.matchers.planning.QueryProcessingException;
import tools.refinery.interpreter.matchers.psystem.analysis.QueryAnalyzer;
import tools.refinery.interpreter.matchers.psystem.queries.PQueries;
import tools.refinery.interpreter.matchers.psystem.queries.PQuery;
import tools.refinery.interpreter.matchers.util.CollectionsFactory;
import tools.refinery.interpreter.matchers.util.IMultiLookup;
import tools.refinery.interpreter.matchers.util.Preconditions;
import tools.refinery.interpreter.util.InterpreterLoggingUtil;

import java.lang.ref.WeakReference;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * A Refinery Interpreter engine back-end (implementation)
 *
 * @author Bergmann Gábor
 */
public final class InterpreterEngineImpl extends AdvancedInterpreterEngine
        implements IQueryBackendHintProvider, IQueryCacheContext, IQueryResultProviderAccess {

    /**
     *
     */
    private static final String ERROR_ACCESSING_BACKEND = "Error while accessing query evaluator backend";
    /**
     *
     */
    private static final String QUERY_ON_DISPOSED_ENGINE_MESSAGE = "Cannot evaluate query on disposed engine!";

    /**
     * The model to which the engine is attached.
     */
    private final QueryScope scope;

    /**
     * The context of the engine, provided by the scope.
     */
    private final IEngineContext engineContext;

    /**
     * Initialized matchers for each query
     */
    private final IMultiLookup<IQuerySpecification<? extends InterpreterMatcher<?>>, InterpreterMatcher<?>> matchers =
            CollectionsFactory.createMultiLookup(Object.class, CollectionsFactory.MemoryType.SETS, Object.class);

    /**
     * The RETE and other pattern matcher implementations of the Refinery Interpreter engine.
     */
    private final Map<IQueryBackendFactory, IQueryBackend> queryBackends = Collections.synchronizedMap(new HashMap<>());

    /**
     * The current engine default hints
     */
    private final InterpreterEngineOptions engineOptions;

    /**
     * Common query analysis provided to backends
     */
    private QueryAnalyzer queryAnalyzer;

    /**
     * true if message delivery is currently delayed, false otherwise
     */
    private boolean delayMessageDelivery = true;

    private final LifecycleProvider lifecycleProvider;
    private final ModelUpdateProvider modelUpdateProvider;
    private Logger logger;
    private boolean disposed = false;

    /**
     * @param scope
     * @param engineDefaultHint
     * @since 1.4
     */
    public InterpreterEngineImpl(QueryScope scope,
								 InterpreterEngineOptions engineOptions) {
        super();
        this.scope = scope;
        this.lifecycleProvider = new LifecycleProvider(this, getLogger());
        this.modelUpdateProvider = new ModelUpdateProvider(this, getLogger());
        this.engineContext = scope.createEngineContext(this, taintListener, getLogger());

        if (engineOptions != null) {
            this.engineOptions = engineOptions;
        } else {
            this.engineOptions = InterpreterEngineOptions.getDefault();
        }

    }

    /**
     * @param manager
     *            null if unmanaged
     * @param scope
     * @param engineDefaultHint
     */
    public InterpreterEngineImpl(QueryScope scope) {
        this(scope, InterpreterEngineOptions.getDefault());
    }

    @Override
    public boolean isUpdatePropagationDelayed() {
        return this.delayMessageDelivery;
    }

    @Override
    public <V> V delayUpdatePropagation(Callable<V> callable) throws InvocationTargetException {
		if (!delayMessageDelivery) {
			throw new IllegalStateException("Trying to delay propagation while changes are being flushed");
		}
        try {
            return callable.call();
        } catch (Exception e) {
            throw new InvocationTargetException(e);
        }
    }

	@Override
	public void flushChanges() {
		if (!delayMessageDelivery) {
			throw new IllegalStateException("Trying to flush changes while changes are already being flushed");
		}
		delayMessageDelivery = false;
		try {
			flushAllBackends();
		} finally {
			delayMessageDelivery = true;
		}
	}

	private void flushAllBackends() {
		for (IQueryBackend backend : this.queryBackends.values()) {
			backend.flushUpdates();
		}
	}

	@Override
	public <T> T withFlushingChanges(Supplier<T> callback) {
		if (!delayMessageDelivery) {
			return callback.get();
		}
		delayMessageDelivery = false;
		try {
			flushAllBackends();
			return callback.get();
		} finally {
			delayMessageDelivery = true;
		}
	}

	@Override
    public Set<? extends InterpreterMatcher<? extends IPatternMatch>> getCurrentMatchers() {
        return matchers.distinctValuesStream().collect(Collectors.toSet());
    }

    @Override
    public <Matcher extends InterpreterMatcher<? extends IPatternMatch>> Matcher getMatcher(
            IQuerySpecification<Matcher> querySpecification) {
        return getMatcher(querySpecification, null);
    }

    @Override
    public <Matcher extends InterpreterMatcher<? extends IPatternMatch>> Matcher getMatcher(
            IQuerySpecification<Matcher> querySpecification, QueryEvaluationHint optionalEvaluationHints) {
		return withFlushingChanges(() -> {
			IMatcherCapability capability = getRequestedCapability(querySpecification, optionalEvaluationHints);
			Matcher matcher = doGetExistingMatcher(querySpecification, capability);
			if (matcher != null) {
				return matcher;
			}
			matcher = querySpecification.instantiate();

			BaseMatcher<?> baseMatcher = (BaseMatcher<?>) matcher;
			((QueryResultWrapper) baseMatcher).setBackend(this,
					getResultProvider(querySpecification, optionalEvaluationHints), capability);
			internalRegisterMatcher(querySpecification, baseMatcher);
			return matcher;
		});
    }

    @Override
    public <Matcher extends InterpreterMatcher<? extends IPatternMatch>> Matcher getExistingMatcher(
            IQuerySpecification<Matcher> querySpecification) {
        return getExistingMatcher(querySpecification, null);
    }

    @Override
    public <Matcher extends InterpreterMatcher<? extends IPatternMatch>> Matcher getExistingMatcher(
            IQuerySpecification<Matcher> querySpecification, QueryEvaluationHint optionalOverrideHints) {
        return doGetExistingMatcher(querySpecification, getRequestedCapability(querySpecification, optionalOverrideHints));
    }

    @SuppressWarnings("unchecked")
    private <Matcher extends InterpreterMatcher<? extends IPatternMatch>> Matcher doGetExistingMatcher(
            IQuerySpecification<Matcher> querySpecification, IMatcherCapability requestedCapability) {
        for (InterpreterMatcher<?> matcher : matchers.lookupOrEmpty(querySpecification)) {
            BaseMatcher<?> baseMatcher = (BaseMatcher<?>) matcher;
            if (baseMatcher.getCapabilities().canBeSubstitute(requestedCapability))
                return (Matcher) matcher;
        }
        return null;
    }

    @Override
    public InterpreterMatcher<? extends IPatternMatch> getMatcher(String patternFQN) {
		throw new UnsupportedOperationException("Query specification registry is not available");
    }

    @Override
    public IBaseIndex getBaseIndex() {
        return engineContext.getBaseIndex();
    }

    public final Logger getLogger() {
        if (logger == null) {
            final int hash = System.identityHashCode(this);
            logger = Logger.getLogger(InterpreterLoggingUtil.getLogger(InterpreterEngine.class).getName() + "." + hash);
            if (logger == null)
                throw new AssertionError(
                        "Configuration error: unable to create Refinery Interpreter runtime logger for engine " + hash);
        }
        return logger;
    }

    ///////////////// internal stuff //////////////
    private void internalRegisterMatcher(IQuerySpecification<?> querySpecification, InterpreterMatcher<?> matcher) {
        matchers.addPair(querySpecification, matcher);
        lifecycleProvider.matcherInstantiated(matcher);
    }

    /**
     * Provides access to the selected query backend component of the Refinery Interpreter engine.
     */
    @Override
    public IQueryBackend getQueryBackend(IQueryBackendFactory iQueryBackendFactory) {
        IQueryBackend iQueryBackend = queryBackends.get(iQueryBackendFactory);
        if (iQueryBackend == null) {
            // do this first, to make sure the runtime context exists
            final IQueryRuntimeContext queryRuntimeContext = engineContext.getQueryRuntimeContext();

            // maybe the backend has been created in the meantime when the indexer was initialized and queried for
            // derived features
            // no need to instantiate a new backend in that case
            iQueryBackend = queryBackends.get(iQueryBackendFactory);
            if (iQueryBackend == null) {

                // need to instantiate the backend
                iQueryBackend = iQueryBackendFactory.create(new IQueryBackendContext() {

                    @Override
                    public IQueryRuntimeContext getRuntimeContext() {
                        return queryRuntimeContext;
                    }

                    @Override
                    public IQueryCacheContext getQueryCacheContext() {
                        return InterpreterEngineImpl.this;
                    }

                    @Override
                    public Logger getLogger() {
                        return logger;
                    }

                    @Override
                    public IQueryBackendHintProvider getHintProvider() {
                        return InterpreterEngineImpl.this;
                    }

                    @Override
                    public IQueryResultProviderAccess getResultProviderAccess() {
                        return InterpreterEngineImpl.this;
                    }

                    @Override
                    public QueryAnalyzer getQueryAnalyzer() {
                        if (queryAnalyzer == null)
                            queryAnalyzer = new QueryAnalyzer(queryRuntimeContext.getMetaContext());
                        return queryAnalyzer;
                    }

                    @Override
                    public boolean areUpdatesDelayed() {
                        return InterpreterEngineImpl.this.delayMessageDelivery;
                    }

                    @Override
                    public IMatcherCapability getRequiredMatcherCapability(PQuery query,
                                                                           QueryEvaluationHint hint) {
                        return engineOptions.getQueryBackendFactory(hint).calculateRequiredCapability(query, hint);
                    }



                });
                queryBackends.put(iQueryBackendFactory, iQueryBackend);
            }
        }
        return iQueryBackend;
    }

    ///////////////// advanced stuff /////////////

    @Override
    public void dispose() {
        wipe();

        this.disposed = true;

        // called before base index disposal to allow removal of base listeners
        lifecycleProvider.engineDisposed();

        try {
            engineContext.dispose();
        } catch (IllegalStateException ex) {
            getLogger().warn(
                    "The base index could not be disposed along with the Refinery Interpreter engine, as there are " +
							"still active listeners on it.");
        }
    }

    @Override
    public void wipe() {
		for (IQueryBackend backend : queryBackends.values()) {
			backend.dispose();
		}
		queryBackends.clear();
        matchers.clear();
        queryAnalyzer = null;
        lifecycleProvider.engineWiped();
    }

    /**
     * Indicates whether the engine is in a tainted, inconsistent state.
     */
    private boolean tainted = false;
    private IIndexingErrorListener taintListener = new SelfTaintListener(this);

    private static class SelfTaintListener implements IIndexingErrorListener {
        WeakReference<InterpreterEngineImpl> queryEngineRef;

        public SelfTaintListener(InterpreterEngineImpl queryEngine) {
            this.queryEngineRef = new WeakReference<InterpreterEngineImpl>(queryEngine);
        }

        public void engineBecameTainted(String description, Throwable t) {
            final InterpreterEngineImpl queryEngine = queryEngineRef.get();
            if (queryEngine != null) {
                queryEngine.tainted = true;
                queryEngine.lifecycleProvider.engineBecameTainted(description, t);
            }
        }

        private boolean noTaintDetectedYet = true;

        protected void notifyTainted(String description, Throwable t) {
            if (noTaintDetectedYet) {
                noTaintDetectedYet = false;
                engineBecameTainted(description, t);
            }
        }

        @Override
        public void error(String description, Throwable t) {
            // Errors does not mean tainting
        }

        @Override
        public void fatal(String description, Throwable t) {
            notifyTainted(description, t);
        }
    }

    @Override
    public boolean isTainted() {
        return tainted;
    }

    private <Match extends IPatternMatch> IQueryResultProvider getUnderlyingResultProvider(
            final BaseMatcher<Match> matcher) {
        // IQueryResultProvider resultProvider = reteEngine.accessMatcher(matcher.getSpecification());
        return matcher.backend;
    }

    @Override
    public <Match extends IPatternMatch> void addMatchUpdateListener(final InterpreterMatcher<Match> matcher,
            final IMatchUpdateListener<? super Match> listener, boolean fireNow) {

        Preconditions.checkArgument(listener != null, "Cannot add null listener!");
        Preconditions.checkArgument(matcher.getEngine() == this, "Cannot register listener for matcher of different engine!");
        Preconditions.checkArgument(!disposed, "Cannot register listener on matcher of disposed engine!");

        final BaseMatcher<Match> bm = (BaseMatcher<Match>) matcher;

        final IUpdateable updateDispatcher = (updateElement, isInsertion) -> {
            Match match = null;
            try {
                match = bm.newMatch(updateElement.getElements());
                if (isInsertion)
                    listener.notifyAppearance(match);
                else
                    listener.notifyDisappearance(match);
            } catch (Throwable e) { // NOPMD
                if (e instanceof Error)
                    throw (Error) e;
                logger.warn(
                        String.format(
                                "The incremental pattern matcher encountered an error during %s a callback on %s of match %s of pattern %s. Error message: %s. (Developer note: %s in %s callback)",
                                match == null ? "preparing" : "invoking", isInsertion ? "insertion" : "removal",
                                match == null ? updateElement.toString() : match.prettyPrint(),
                                matcher.getPatternName(), e.getMessage(), e.getClass().getSimpleName(), listener),
                        e);
            }

        };

        IQueryResultProvider resultProvider = getUnderlyingResultProvider(bm);
        resultProvider.addUpdateListener(updateDispatcher, listener, fireNow);
    }

    @Override
    public <Match extends IPatternMatch> void removeMatchUpdateListener(InterpreterMatcher<Match> matcher,
																		IMatchUpdateListener<? super Match> listener) {
        Preconditions.checkArgument(listener != null, "Cannot remove null listener!");
        Preconditions.checkArgument(matcher.getEngine() == this, "Cannot remove listener from matcher of different engine!");
        Preconditions.checkArgument(!disposed, "Cannot remove listener from matcher of disposed engine!");

        final BaseMatcher<Match> bm = (BaseMatcher<Match>) matcher;

        try {
            IQueryResultProvider resultProvider = getUnderlyingResultProvider(bm);
            resultProvider.removeUpdateListener(listener);
        } catch (Exception e) {
            logger.error(
                    "Error while removing listener " + listener + " from the matcher of " + matcher.getPatternName(),
                    e);
        }
    }

    @Override
    public void addModelUpdateListener(InterpreterModelUpdateListener listener) {
        modelUpdateProvider.addListener(listener);
    }

    @Override
    public void removeModelUpdateListener(InterpreterModelUpdateListener listener) {
        modelUpdateProvider.removeListener(listener);
    }

    @Override
    public void addLifecycleListener(InterpreterEngineLifecycleListener listener) {
        lifecycleProvider.addListener(listener);
    }

    @Override
    public void removeLifecycleListener(InterpreterEngineLifecycleListener listener) {
        lifecycleProvider.removeListener(listener);
    }

    /**
     * Returns an internal interface towards the query backend to feed the matcher with results.
     *
     * @param query
     *            the pattern for which the result provider should be delivered
     *
     * @throws InterpreterRuntimeException
     */
    public IQueryResultProvider getResultProvider(IQuerySpecification<?> query) {
        Preconditions.checkState(!disposed, QUERY_ON_DISPOSED_ENGINE_MESSAGE);

        return getResultProviderInternal(query, null);
    }

    /**
     * Returns an internal interface towards the query backend to feed the matcher with results.
     *
     * @param query
     *            the pattern for which the result provider should be delivered
     *
     * @throws InterpreterRuntimeException
     */
    public IQueryResultProvider getResultProvider(IQuerySpecification<?> query, QueryEvaluationHint hint) {
        Preconditions.checkState(!disposed, QUERY_ON_DISPOSED_ENGINE_MESSAGE);

        return getResultProviderInternal(query, hint);
    }

    /**
     * This method returns the result provider exactly as described by the passed hint. Query cannot be null! Use
     * {@link #getQueryEvaluationHint(IQuerySpecification, QueryEvaluationHint)} before passing a hint to this method to
     * make sure engine and query specific hints are correctly applied.
     *
     * @throws InterpreterRuntimeException
     */
    private IQueryResultProvider getResultProviderInternal(IQuerySpecification<?> query, QueryEvaluationHint hint) {
        return getResultProviderInternal(query.getInternalQueryRepresentation(), hint);
    }

    /**
     * This method returns the result provider exactly as described by the passed hint. Query cannot be null! Use
     * {@link #getQueryEvaluationHint(IQuerySpecification, QueryEvaluationHint)} before passing a hint to this method to
     * make sure engine and query specific hints are correctly applied.
     *
     * @throws InterpreterRuntimeException
     */
    private IQueryResultProvider getResultProviderInternal(PQuery query, QueryEvaluationHint hint) {
        Preconditions.checkArgument(query != null, "Query cannot be null!");
        Preconditions.checkArgument(query.getStatus() != PQuery.PQueryStatus.ERROR, "Cannot initialize a result provider for the erronoues query `%s`.", query.getSimpleName());
        final IQueryBackend backend = getQueryBackend(engineOptions.getQueryBackendFactory(getQueryEvaluationHint(query, hint)));
        return backend.getResultProvider(query, hint);
    }

    /**
     * Returns the query backend (influenced by the hint system), even if it is a non-caching backend.
     *
     * @throws InterpreterRuntimeException
     */
    private IQueryBackend getQueryBackend(PQuery query) {
        final IQueryBackendFactory factory = engineOptions.getQueryBackendFactory(getQueryEvaluationHint(query));
        return getQueryBackend(factory);
    }

    /**
     * Returns a caching query backend (influenced by the hint system).
     *
     * @throws InterpreterRuntimeException
     */
    private IQueryBackend getCachingQueryBackend(PQuery query) {
        IQueryBackend regularBackend = getQueryBackend(query);
        if (regularBackend.isCaching())
            return regularBackend;
        else
            return getQueryBackend(engineOptions.getDefaultCachingBackendFactory());
    }

    @Override
    public boolean isResultCached(PQuery query) {
        try {
            return null != getCachingQueryBackend(query).peekExistingResultProvider(query);
        } catch (InterpreterException iqe) {
            getLogger().error(ERROR_ACCESSING_BACKEND, iqe);
            return false;
        }
    }

    @Override
    public IQueryResultProvider getCachingResultProvider(PQuery query) {
        try {
            return getCachingQueryBackend(query).getResultProvider(query);
        } catch (InterpreterException iqe) {
            getLogger().error(ERROR_ACCESSING_BACKEND, iqe);
            throw iqe;
        }
    }

    private QueryEvaluationHint getEngineDefaultHint() {
        return engineOptions.getEngineDefaultHints();
    }

    @Override
    public QueryEvaluationHint getQueryEvaluationHint(PQuery query) {
        return getEngineDefaultHint().overrideBy(query.getEvaluationHints());
    }

    private QueryEvaluationHint getQueryEvaluationHint(IQuerySpecification<?> querySpecification,
            QueryEvaluationHint optionalOverrideHints) {
        return getQueryEvaluationHint(querySpecification.getInternalQueryRepresentation())
                        .overrideBy(optionalOverrideHints);
    }

    private QueryEvaluationHint getQueryEvaluationHint(PQuery query, QueryEvaluationHint optionalOverrideHints) {
        return getQueryEvaluationHint(query).overrideBy(optionalOverrideHints);
    }

    private IMatcherCapability getRequestedCapability(IQuerySpecification<?> querySpecification,
            QueryEvaluationHint optionalOverrideHints) {
        final QueryEvaluationHint hint = getQueryEvaluationHint(querySpecification, optionalOverrideHints);
        return engineOptions.getQueryBackendFactory(hint)
                .calculateRequiredCapability(querySpecification.getInternalQueryRepresentation(), hint);
    }

    @Override
    public void prepareGroup(IQueryGroup queryGroup, final QueryEvaluationHint optionalEvaluationHints) {
        try {
            Preconditions.checkState(!disposed, QUERY_ON_DISPOSED_ENGINE_MESSAGE);

            final Set<IQuerySpecification<?>> specifications = new HashSet<IQuerySpecification<?>>(
                    queryGroup.getSpecifications());
            final Collection<PQuery> patterns = specifications.stream().map(
                    IQuerySpecification::getInternalQueryRepresentation).collect(Collectors.toList());
            patterns.forEach(PQuery::ensureInitialized);

            Collection<String> erroneousPatterns = patterns.stream().
                    filter(PQueries.queryStatusPredicate(PQuery.PQueryStatus.ERROR)).
                    map(PQuery::getFullyQualifiedName).
                    collect(Collectors.toList());
            Preconditions.checkState(erroneousPatterns.isEmpty(), "Erroneous query(s) found: %s",
                    erroneousPatterns.stream().collect(Collectors.joining(", ")));

            // TODO maybe do some smarter preparation per backend?
            try {
                engineContext.getBaseIndex().coalesceTraversals(new Callable<Void>() {
                    @Override
                    public Void call() throws Exception {
                        for (IQuerySpecification<?> query : specifications) {
                            getResultProviderInternal(query, optionalEvaluationHints);
                        }
                        return null;
                    }
                });
            } catch (InvocationTargetException ex) {
                final Throwable cause = ex.getCause();
                if (cause instanceof QueryProcessingException)
                    throw (QueryProcessingException) cause;
                if (cause instanceof InterpreterException)
                    throw (InterpreterException) cause;
                if (cause instanceof RuntimeException)
                    throw (RuntimeException) cause;
                assert (false);
            }
        } catch (QueryProcessingException e) {
            throw new InterpreterException(e);
        }
    }

    @Override
    public QueryScope getScope() {
        return scope;
    }

    @Override
    public InterpreterEngineOptions getEngineOptions() {
        return engineOptions;
    }

    @Override
    public IQueryResultProvider getResultProviderOfMatcher(InterpreterMatcher<? extends IPatternMatch> matcher) {
        return ((QueryResultWrapper) matcher).backend;
    }

    @Override
    public IQueryResultProvider getResultProvider(PQuery query, QueryEvaluationHint overrideHints) {
        try {
            return getResultProviderInternal(query, overrideHints);
        } catch (InterpreterException e) {
            getLogger().error(ERROR_ACCESSING_BACKEND, e);
            throw e;
        }
    }

    @Override
    public boolean isDisposed() {
        return disposed;
    }

}
