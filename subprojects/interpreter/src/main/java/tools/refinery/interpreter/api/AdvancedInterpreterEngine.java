/*******************************************************************************
 * Copyright (c) 2010-2013, Bergmann Gabor, Istvan Rath and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.interpreter.api;

import tools.refinery.interpreter.api.scope.QueryScope;
import tools.refinery.interpreter.internal.apiimpl.InterpreterEngineImpl;
import tools.refinery.interpreter.matchers.InterpreterRuntimeException;
import tools.refinery.interpreter.matchers.backend.*;

import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.Callable;

/**
 * Advanced interface to a Refinery Interpreter incremental evaluation engine.
 *
 * <p>
 * You can create a new, private, unmanaged {@link AdvancedInterpreterEngine} instance using
 * {@link #createUnmanagedEngine(QueryScope)}. Additionally, you can access the advanced interface on any
 * {@link InterpreterEngine} by {@link AdvancedInterpreterEngine#from(InterpreterEngine)}.
 *
 * <p>
 * While the default interface {@link InterpreterEngine}, is suitable for most users, this advanced interface provides more
 * control over the engine. The most important added functionality is the following:
 * <ul>
 * <li>You can have tighter control over the lifecycle of the engine, if you create a private, unmanaged engine
 * instance. For instance, a (non-managed) engine can be disposed in order to detach from the EMF model and stop
 * listening on update notifications. The indexes built previously in the engine can then be garbage collected, even if
 * the model itself is retained. Total lifecycle control is only available for private, unmanaged engines (created using
 * {@link #createUnmanagedEngine(QueryScope)}); a managed engine (obtained via {@link InterpreterEngine#on(QueryScope)}) is
 * shared among clients and can not be disposed or wiped.
 * <li>You can add and remove listeners to receive notification when the model or the match sets change.
 * <li>You can add and remove listeners to receive notification on engine lifecycle events, such as creation of new
 * matchers. For instance, if you explicitly share a private, unmanaged engine between multiple sites, you should
 * register a callback using {@link #addLifecycleListener(InterpreterEngineLifecycleListener)} to learn when another client
 * has called the destructive methods {@link #dispose()} or {@link #wipe()}.
 * </ul>
 *
 * @author Bergmann Gabor
 * @noextend This class is not intended to be subclassed by clients.
 */
public abstract class AdvancedInterpreterEngine extends InterpreterEngine {

    /**
     * Creates a new unmanaged Refinery Interpreter engine to evaluate queries over a given scope specified by an
	 * {@link QueryScope}.
     *
     * <p> Repeated invocations will return different instances, so other clients are unable to independently access
     * and influence the returned engine. Note that unmanaged engines do not benefit from some performance improvements
     * that stem from sharing incrementally maintained indices and caches between multiple clients using the same managed
     * engine instance.
     *
     * <p>
     * Client is responsible for the lifecycle of the returned engine, hence the usage of the advanced interface
     * {@link AdvancedInterpreterEngine}.
     *
     * <p>
     * The match set of any patterns will be incrementally refreshed upon updates from this scope.
     *
     * @param scope
     * 		the scope of query evaluation; the definition of the set of model elements that this engine is operates on.
     * 		Provide e.g. a {@link EMFScope} for evaluating queries on an EMF model.
     * @return the advanced interface to a newly created unmanaged engine
     * @since 0.9
     */
    public static AdvancedInterpreterEngine createUnmanagedEngine(QueryScope scope) {
        return new InterpreterEngineImpl(scope);
    }

    /**
     * Creates a new unmanaged Refinery Intepreter engine to evaluate queries over a given scope specified by an
	 * {@link QueryScope}.
     *
     * <p> Repeated invocations will return different instances, so other clients are unable to independently access
     * and influence the returned engine. Note that unmanaged engines do not benefit from some performance improvements
     * that stem from sharing incrementally maintained indices and caches between multiple clients using the same managed
     * engine instance.
     *
     * <p>
     * Client is responsible for the lifecycle of the returned engine, hence the usage of the advanced interface
     * {@link AdvancedInterpreterEngine}.
     *
     * <p>
     * The match set of any patterns will be incrementally refreshed upon updates from this scope.
     *
     * @param scope
     *      the scope of query evaluation; the definition of the set of model elements that this engine is operates on.
     *      Provide e.g. a {@link EMFScope} for evaluating queries on an EMF model.
     * @return the advanced interface to a newly created unmanaged engine
     * @since 1.4
     */
    public static AdvancedInterpreterEngine createUnmanagedEngine(QueryScope scope, InterpreterEngineOptions options) {
        return new InterpreterEngineImpl(scope, options);
    }

    /**
     * Provides access to a given existing engine through the advanced interface.
     *
     * @param engine
     *            the engine to access using the advanced interface
     * @return a reference to the same engine conforming to the advanced interface
     */
    public static AdvancedInterpreterEngine from(InterpreterEngine engine) {
        return (AdvancedInterpreterEngine) engine;
    }

    /**
     * Add an engine lifecycle listener to this engine instance.
     *
     * @param listener
     *            the {@link InterpreterEngineLifecycleListener} that should listen to lifecycle events from this engine
     */
    public abstract void addLifecycleListener(InterpreterEngineLifecycleListener listener);

    /**
     * Remove an existing lifecycle listener from this engine instance.
     *
     * @param listener
     *            the {@link InterpreterEngineLifecycleListener} that should not listen to lifecycle events from this
     *            engine anymore
     */
    public abstract void removeLifecycleListener(InterpreterEngineLifecycleListener listener);

    /**
     * Add an model update event listener to this engine instance (that fires its callbacks according to its
     * notification level).
     *
     * @param listener
     *            the {@link InterpreterModelUpdateListener} that should listen to model update events from this engine.
     */
    public abstract void addModelUpdateListener(InterpreterModelUpdateListener listener);

    /**
     * Remove an existing model update event listener to this engine instance.
     *
     * @param listener
     *            the {@link InterpreterModelUpdateListener} that should not listen to model update events from this engine
     *            anymore
     */
    public abstract void removeModelUpdateListener(InterpreterModelUpdateListener listener);

    /**
     * Registers low-level callbacks for match appearance and disappearance on this pattern matcher.
     *
     * <p>
     * <b>Caution: </b> This is a low-level callback that is invoked when the pattern matcher is not necessarily in a
     * consistent state yet. Importantly, no model modification permitted during the callback. Most users should use the
     * databinding support ({@link org.eclipse.viatra.addon.databinding.runtime.api.ViatraObservables ViatraObservables}) or the event-driven API
     * ({@link org.eclipse.viatra.transformation.evm.api.EventDrivenVM EventDrivenVM}) instead.
     *
     * <p>
     * Performance note: expected to be much more efficient than polling at {@link #addCallbackAfterUpdates(Runnable)},
     * but prone to "signal hazards", e.g. spurious match appearances that will disappear immediately afterwards.
     *
     * <p>
     * The callback can be unregistered via {@link #removeCallbackOnMatchUpdate(IMatchUpdateListener)}.
     *
     * @param fireNow
     *            if true, appearCallback will be immediately invoked on all current matches as a one-time effect. See
     *            also {@link InterpreterMatcher#forEachMatch(IMatchProcessor)}.
     * @param listener
     *            the listener that will be notified of each new match that appears or disappears, starting from now.
     * @param matcher
     *            the {@link InterpreterMatcher} for which this listener should be active
     */
    public abstract <Match extends IPatternMatch> void addMatchUpdateListener(InterpreterMatcher<Match> matcher,
																			  IMatchUpdateListener<? super Match> listener, boolean fireNow);

    /**
     * Remove an existing match update event listener to this engine instance.
     *
     * @param matcher
     *            the {@link InterpreterMatcher} for which this listener should not be active anymore
     * @param listener
     *            the {@link IMatchUpdateListener} that should not receive the callbacks anymore
     */
    public abstract <Match extends IPatternMatch> void removeMatchUpdateListener(InterpreterMatcher<Match> matcher,
																				 IMatchUpdateListener<? super Match> listener);


    /**
     * Access a pattern matcher based on a {@link IQuerySpecification}, overriding some of the default query evaluation hints.
     * Multiple calls may return the same matcher depending on the actual evaluation hints.
     *
     * <p> It is guaranteed that this method will always return a matcher instance which is functionally compatible
     *   with the requested functionality (see {@link IMatcherCapability}).
     *   Otherwise, the query evaluator is free to ignore any hints.
     *
     * <p> For stateful query backends (Rete), hints may be effective only the first time a matcher is created.
     * @param querySpecification a {@link IQuerySpecification} that describes a Refinery Interpreter query
     * @return a pattern matcher corresponding to the specification
     * @param optionalEvaluationHints additional / overriding options on query evaluation; passing null means default options associated with the query
     * @throws InterpreterRuntimeException if the matcher could not be initialized
     * @since 0.9
     */
    public abstract <Matcher extends InterpreterMatcher<? extends IPatternMatch>> Matcher getMatcher(
            IQuerySpecification<Matcher> querySpecification,
            QueryEvaluationHint optionalEvaluationHints);

    /**
     * Initializes matchers for a group of patterns as one step (optionally overriding some of the default query evaluation hints).
     * If some of the pattern matchers are already
     * constructed in the engine, no task is performed for them.
     *
     * <p>
     * This preparation step has the advantage that it prepares pattern matchers for an arbitrary number of patterns in a
     * single-pass traversal of the model.
     * This is typically more efficient than traversing the model each time an individual pattern matcher is initialized on demand.
     * The performance benefit only manifests itself if the engine is not in wildcard mode.
     *
     * @param queryGroup a {@link IQueryGroup} identifying a set of Refinery interpreter queries
     * @param optionalEvaluationHints additional / overriding options on query evaluation; passing null means default options associated with each query
     * @throws InterpreterRuntimeException
     *             if there was an error in preparing the engine
     * @since 0.9
     */
    public abstract void prepareGroup(IQueryGroup queryGroup, QueryEvaluationHint optionalEvaluationHints);

    /**
     * Indicates whether the engine is in a tainted, inconsistent state due to some internal errors. If true, results
     * are no longer reliable; engine should be disposed.
     *
     * <p>
     * The engine is in a tainted state if any of its internal processes report back a fatal error. The
     * {@link InterpreterEngineLifecycleListener} interface provides a callback method for entering the tainted state.
     *
     * @return the tainted state
     */
    public abstract boolean isTainted();

    /**
     * Discards any pattern matcher caches and forgets known patterns. The base index built directly on the underlying
     * EMF model, however, is kept in memory to allow reuse when new pattern matchers are built. Use this method if you
     * have e.g. new versions of the same patterns, to be matched on the same model.
     *
     * <p>
     * Matcher objects will continue to return stale results. If no references are retained to the matchers, they can
     * eventually be GC'ed.
     * <p>
     * Disallowed if the engine is managed (see {@link #isManaged()}), as there may be other clients using it.
     * <p>
     * If you explicitly share a private, unmanaged engine between multiple sites, register a callback using
     * {@link #addLifecycleListener(InterpreterEngineLifecycleListener)} to learn when another client has called this
     * destructive method.
     *
     * @throws UnsupportedOperationException
     *             if engine is managed
     */
    public abstract void wipe();

    /**
     * Completely disconnects and dismantles the engine. Cannot be reversed.
     * <p>
     * Matcher objects will continue to return stale results. If no references are retained to the matchers or the
     * engine, they can eventually be GC'ed, and they won't block the EMF model from being GC'ed anymore.
     * <p>
     * The base indexer (see {@link #getBaseIndex()}) built on the model will be disposed alongside the engine, unless
     * the user has manually added listeners on the base index that were not removed yet.
     * <p>
     * Disallowed if the engine is managed (see {@link #isManaged()}), as there may be other clients using it.
     * <p>
     * If you explicitly share a private, unmanaged engine between multiple sites, register a callback using
     * {@link #addLifecycleListener(InterpreterEngineLifecycleListener)} to learn when another client has called this
     * destructive method.
     *
     * @throws UnsupportedOperationException
     *             if engine is managed
     */
    public abstract void dispose();

    /**
     * Provides access to the selected query backend component of the Refinery Interpreter engine.
     * @noreference for internal use only
     * @throws InterpreterRuntimeException
     */
    public abstract IQueryBackend getQueryBackend(IQueryBackendFactory iQueryBackendFactory);

    /**
     * Access an existing pattern matcher based on a {@link IQuerySpecification}, and optional hints override.
     * @param querySpecification a {@link IQuerySpecification} that describes a Refinery Interpreter query specification
     * @param optionalOverrideHints a {@link QueryEvaluationHint} that may override the pattern hints (can be null)
     * @return a pattern matcher corresponding to the specification, <code>null</code> if a matcher does not exist yet.
     * @since 1.4
     */
    public abstract <Matcher extends InterpreterMatcher<? extends IPatternMatch>> Matcher getExistingMatcher(IQuerySpecification<Matcher> querySpecification, QueryEvaluationHint optionalOverrideHints);

    /**
     * Returns the immutable {@link InterpreterEngineOptions} of the engine.
     *
     * @return the engine options
     * @since 1.4
     */
    public abstract InterpreterEngineOptions getEngineOptions();

    /**
     * Return the underlying result provider for the given matcher.
     *
     * @beta This method may change in future versions
     * @since 1.4
     * @noreference This method is considered internal API
     */
    public abstract IQueryResultProvider getResultProviderOfMatcher(InterpreterMatcher<? extends IPatternMatch> matcher);

    /**
     * The given callable will be executed, and all update propagation in stateful query backends
     * will be delayed until the execution is done. Within the callback, these backends will provide stale results.
     *
     * <p> It is optional for a {@link IQueryBackend} to support the delaying of update propagation; stateless backends will display up-to-date results.
     * In this case, the given callable shall be executed, and the update propagation shall happen just like in non-delayed execution.
     *
     * <p> Example: in the Rete network, no messages will be propagated until the given callable is executed.
     * After the execution of the callable, all accumulated messages will be delivered.
     *
     * <p> The purpose of this method is that stateful query backends may save work when multiple model modifications are performed within the callback that partially cancel each other out.
     *
     * @param callable the callable to be executed
     * @return the result of the callable
     * @since 1.6
     */
    public abstract <V> V delayUpdatePropagation(Callable<V> callable) throws InvocationTargetException;

    /**
     * Returns true if the update propagation in this engine is currently delayed, false otherwise.
     *
     * @see {@link #delayUpdatePropagation(Callable)}
     * @since 1.6
     */
    public abstract boolean isUpdatePropagationDelayed();

    /**
     * Returns true if the {@link #dispose()} method was called on this engine previously.
     * @since 2.0
     */
    public abstract boolean isDisposed();
}
