/*******************************************************************************
 * Copyright (c) 2004-2008 Gabor Bergmann and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package tools.refinery.interpreter.rete.matcher;

import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.Callable;

import org.apache.log4j.Logger;
import tools.refinery.interpreter.rete.boundary.Disconnectable;
import tools.refinery.interpreter.rete.boundary.ReteBoundary;
import tools.refinery.interpreter.rete.construction.RetePatternBuildException;
import tools.refinery.interpreter.rete.construction.plancompiler.ReteRecipeCompiler;
import tools.refinery.interpreter.rete.network.Network;
import tools.refinery.interpreter.rete.network.NodeProvisioner;
import tools.refinery.interpreter.rete.network.ReteContainer;
import tools.refinery.interpreter.matchers.InterpreterRuntimeException;
import tools.refinery.interpreter.matchers.backend.IQueryBackend;
import tools.refinery.interpreter.matchers.backend.IQueryBackendFactory;
import tools.refinery.interpreter.matchers.backend.IQueryBackendHintProvider;
import tools.refinery.interpreter.matchers.backend.IQueryResultProvider;
import tools.refinery.interpreter.matchers.backend.QueryEvaluationHint;
import tools.refinery.interpreter.matchers.context.IQueryBackendContext;
import tools.refinery.interpreter.matchers.context.IQueryRuntimeContext;
import tools.refinery.interpreter.matchers.psystem.queries.PQuery;
import tools.refinery.interpreter.matchers.tuple.TupleMask;
import tools.refinery.interpreter.matchers.util.CollectionsFactory;
import tools.refinery.interpreter.rete.index.Indexer;
import tools.refinery.interpreter.rete.traceability.RecipeTraceInfo;

/**
 * @author Gabor Bergmann
 *
 */
public class ReteEngine implements IQueryBackend {

    protected Network reteNet;
    protected final int reteThreads;
    protected ReteBoundary boundary;

    /**
     * @since 2.2
     */
    protected final boolean deleteAndRederiveEvaluation;
    /**
     * @since 2.4
     */
    protected final TimelyConfiguration timelyConfiguration;

    private IQueryBackendContext context;
    private Logger logger;
    protected IQueryRuntimeContext runtimeContext;

    protected Collection<Disconnectable> disconnectables;

    protected Map<PQuery, RetePatternMatcher> matchers;

    protected ReteRecipeCompiler compiler;

    protected final boolean parallelExecutionEnabled; // TRUE if model manipulation can go on

    private boolean disposedOrUninitialized = true;

    private HintConfigurator hintConfigurator;

    /**
     * @param context
     *            the context of the pattern matcher, conveying all information from the outside world.
     * @param reteThreads
     *            the number of threads to operate the RETE network with; 0 means single-threaded operation, 1 starts an
     *            asynchronous thread to operate the RETE net, >1 uses multiple RETE containers.
     */
    public ReteEngine(IQueryBackendContext context, int reteThreads) {
        this(context, reteThreads, false, null);
    }

    /**
     * @since 2.4
     */
    public ReteEngine(IQueryBackendContext context, int reteThreads, boolean deleteAndRederiveEvaluation, TimelyConfiguration timelyConfiguration) {
        super();
        this.context = context;
        this.logger = context.getLogger();
        this.runtimeContext = context.getRuntimeContext();
        this.reteThreads = reteThreads;
        this.parallelExecutionEnabled = reteThreads > 0;
        this.deleteAndRederiveEvaluation = deleteAndRederiveEvaluation;
        this.timelyConfiguration = timelyConfiguration;
        initEngine();
        this.compiler = null;
    }

    /**
     * @since 1.6
     */
    public IQueryBackendContext getBackendContext() {
        return context;
    }

    /**
     * @since 2.2
     */
    public boolean isDeleteAndRederiveEvaluation() {
        return this.deleteAndRederiveEvaluation;
    }

    /**
     * @since 2.4
     */
    public TimelyConfiguration getTimelyConfiguration() {
        return this.timelyConfiguration;
    }

    /**
     * initializes engine components
     */
    private synchronized void initEngine() {
        this.disposedOrUninitialized = false;
        this.disconnectables = new LinkedList<Disconnectable>();
        // this.caughtExceptions = new LinkedBlockingQueue<Throwable>();


        this.hintConfigurator = new HintConfigurator(context.getHintProvider());

        this.reteNet = new Network(reteThreads, this);
        this.boundary = new ReteBoundary(this); // prerequisite: network

        this.matchers = CollectionsFactory.createMap();
        /* this.matchersScoped = new HashMap<PatternDescription, Map<Map<Integer,Scope>,RetePatternMatcher>>(); */

        // prerequisite: network, framework, boundary, disconnectables
        //context.subscribeBackendForUpdates(this.boundary);
        // prerequisite: boundary, disconnectables
//        this.traceListener = context.subscribePatternMatcherForTraceInfluences(this);

    }

    @Override
    public void flushUpdates() {
        for (ReteContainer container : this.reteNet.getContainers()) {
            container.deliverMessagesSingleThreaded();
        }
    }

    /**
     * deconstructs engine components
     */
    private synchronized void deconstructEngine() {
        ensureInitialized();
        reteNet.kill();

        //context.unSubscribeBackendFromUpdates(this.boundary);
        for (Disconnectable disc : disconnectables) {
            disc.disconnect();
        }

        this.matchers = null;
        this.disconnectables = null;

        this.reteNet = null;
        this.boundary = null;

        this.hintConfigurator = null;

        // this.machineListener = new MachineListener(this); // prerequisite:
        // framework, disconnectables
//        this.traceListener = null;

        this.disposedOrUninitialized = true;
    }

    /**
     * Deconstructs the engine to get rid of it finally
     */
    public void killEngine() {
        deconstructEngine();
        // this.framework = null;
        this.compiler = null;
        this.logger = null;
    }

    /**
     * Resets the engine to an after-initialization phase
     *
     */
    public void reset() {
        deconstructEngine();

        initEngine();

        compiler.reset();
    }

    /**
     * Accesses the patternmatcher for a given pattern, constructs one if a matcher is not available yet.
     *
     * @pre: builder is set.
     * @param query
     *            the pattern to be matched.
     * @return a patternmatcher object that can match occurences of the given pattern.
     * @throws InterpreterRuntimeException
     *             if construction fails.
     */
    public synchronized RetePatternMatcher accessMatcher(final PQuery query) {
        ensureInitialized();
        RetePatternMatcher matcher;
        // String namespace = gtPattern.getNamespace().getName();
        // String name = gtPattern.getName();
        // String fqn = namespace + "." + name;
        matcher = matchers.get(query);
        if (matcher == null) {
            constructionWrapper(() -> {
                RecipeTraceInfo prodNode;
                prodNode = boundary.accessProductionTrace(query);

                RetePatternMatcher retePatternMatcher = new RetePatternMatcher(ReteEngine.this,
                        prodNode);
                retePatternMatcher.setTag(query);
                matchers.put(query, retePatternMatcher);
                return null;
            });
            matcher = matchers.get(query);
        }

        executeDelayedCommands();

        return matcher;
    }


    /**
     * Constructs RETE pattern matchers for a collection of patterns, if they are not available yet. Model traversal
     * during the whole construction period is coalesced (which may have an effect on performance, depending on the
     * matcher context).
     *
     * @pre: builder is set.
     * @param specifications
     *            the patterns to be matched.
     * @throws InterpreterRuntimeException
     *             if construction fails.
     */
    public synchronized void buildMatchersCoalesced(final Collection<PQuery> specifications) {
        ensureInitialized();
        constructionWrapper(() -> {
            for (PQuery specification : specifications) {
                boundary.accessProductionNode(specification);
            }
            return null;
        });
    }

    /**
     * @since 2.4
     */
    public <T> T constructionWrapper(final Callable<T> payload) {
        T result = null;
//		context.modelReadLock();
//		    try {
                if (parallelExecutionEnabled)
                    reteNet.getStructuralChangeLock().lock();
                try {
                    try {
                        result = runtimeContext.coalesceTraversals(() -> {
                            T innerResult = payload.call();
                            this.executeDelayedCommands();
                            return innerResult;
                        });
                    } catch (InvocationTargetException ex) {
                        final Throwable cause = ex.getCause();
                        if (cause instanceof RetePatternBuildException)
                            throw (RetePatternBuildException) cause;
                        if (cause instanceof RuntimeException)
                            throw (RuntimeException) cause;
                        assert (false);
                    }
                } finally {
                   if (parallelExecutionEnabled)
                        reteNet.getStructuralChangeLock().unlock();
                   reteNet.waitForReteTermination();
                }
//		    } finally {
//		        context.modelReadUnLock();
//		    }
            return result;
    }

    // /**
    // * Accesses the patternmatcher for a given pattern with additional scoping, constructs one if
    // * a matcher is not available yet.
    // *
    // * @param gtPattern
    // * the pattern to be matched.
    // * @param additionalScopeMap
    // * additional, optional scopes for the symbolic parameters
    // * maps the position of the symbolic parameter to its additional scope (if any)
    // * @pre: scope.parent is non-root, i.e. this is a nontrivial constraint
    // * use the static method RetePatternMatcher.buildAdditionalScopeMap() to create from PatternCallSignature
    // * @return a patternmatcher object that can match occurences of the given
    // * pattern.
    // * @throws PatternMatcherCompileTimeException
    // * if construction fails.
    // */
    // public synchronized RetePatternMatcher accessMatcherScoped(PatternDescription gtPattern, Map<Integer, Scope>
    // additionalScopeMap)
    // throws PatternMatcherCompileTimeException {
    // if (additionalScopeMap.isEmpty()) return accessMatcher(gtPattern);
    //
    // RetePatternMatcher matcher;
    //
    // Map<Map<Integer, Scope>, RetePatternMatcher> scopes = matchersScoped.get(gtPattern);
    // if (scopes == null) {
    // scopes = new HashMap<Map<Integer, Scope>, RetePatternMatcher>();
    // matchersScoped.put(gtPattern, scopes);
    // }
    //
    // matcher = scopes.get(additionalScopeMap);
    // if (matcher == null) {
    // context.modelReadLock();
    // try {
    // reteNet.getStructuralChangeLock().lock();
    // try {
    // Address<? extends Production> prodNode;
    // prodNode = boundary.accessProductionScoped(gtPattern, additionalScopeMap);
    //
    // matcher = new RetePatternMatcher(this, prodNode);
    // scopes.put(additionalScopeMap, matcher);
    // } finally {
    // reteNet.getStructuralChangeLock().unlock();
    // }
    // } finally {
    // context.modelReadUnLock();
    // }
    // // reteNet.flushUpdates();
    // }
    //
    // return matcher;
    // }

    /**
     * Returns an indexer that groups the contents of this Production node by their projections to a given mask.
     * Designed to be called by a RetePatternMatcher.
     *
     * @param production
     *            the production node to be indexed.
     * @param mask
     *            the mask that defines the projection.
     * @return the Indexer.
     */
    synchronized Indexer accessProjection(RecipeTraceInfo production, TupleMask mask) {
        ensureInitialized();
        NodeProvisioner nodeProvisioner = reteNet.getHeadContainer().getProvisioner();
        Indexer result = nodeProvisioner.peekProjectionIndexer(production, mask);
        if (result == null) {
            result = constructionWrapper(() ->
                nodeProvisioner.accessProjectionIndexerOnetime(production, mask)
            );
        }

        return result;
    }

    // /**
    // * Retrieves the patternmatcher for a given pattern fqn, returns null if
    // the matching network hasn't been constructed yet.
    // *
    // * @param fqn the fully qualified name of the pattern to be matched.
    // * @return the previously constructed patternmatcher object that can match
    // occurences of the given pattern, or null if it doesn't exist.
    // */
    // public RetePatternMatcher getMatcher(String fqn)
    // {
    // RetePatternMatcher matcher = matchersByFqn.get(fqn);
    // if (matcher == null)
    // {
    // Production prodNode = boundary.getProduction(fqn);
    //
    // matcher = new RetePatternMatcher(this, prodNode);
    // matchersByFqn.put(fqn, matcher);
    // }
    //
    // return matcher;
    // }

    /**
     * @since 2.3
     */
    public void executeDelayedCommands() {
        for (final ReteContainer container : this.reteNet.getContainers()) {
            container.executeDelayedCommands();
        }
    }

    /**
     * Waits until the pattern matcher is in a steady state and output can be retrieved.
     */
    public void settle() {
        ensureInitialized();
        reteNet.waitForReteTermination();
    }

    /**
     * Waits until the pattern matcher is in a steady state and output can be retrieved. When steady state is reached, a
     * retrieval action is executed before the steady state ceases.
     *
     * @param action
     *            the action to be run when reaching the steady-state.
     */
    public void settle(Runnable action) {
        ensureInitialized();
        reteNet.waitForReteTermination(action);
    }

    // /**
    // * @return the framework
    // */
    // public IFramework getFramework() {
    // return framework.get();
    // }

    /**
     * @return the reteNet
     */
    public Network getReteNet() {
       ensureInitialized();
       return reteNet;
    }

    /**
     * @return the boundary
     */
    public ReteBoundary getBoundary() {
        ensureInitialized();
        return boundary;
    }

    // /**
    // * @return the pattern matcher builder
    // */
    // public IRetePatternBuilder getBuilder() {
    // return builder;
    // }

    /**
     * @param builder
     *            the pattern matcher builder to set
     */
    public void setCompiler(ReteRecipeCompiler builder) {
        ensureInitialized();
        this.compiler = builder;
    }

//    /**
//     * @return the manipulationListener
//     */
//    public IManipulationListener getManipulationListener() {
//    	ensureInitialized();
//       return manipulationListener;
//    }

//    /**
//     * @return the traceListener
//     */
//    public IPredicateTraceListener geTraceListener() {
//    	ensureInitialized();
//        return traceListener;
//    }

    /**
     * @param disc
     *            the new Disconnectable adapter.
     */
    public void addDisconnectable(Disconnectable disc) {
        ensureInitialized();
        disconnectables.add(disc);
    }

    /**
     * @return the parallelExecutionEnabled
     */
    public boolean isParallelExecutionEnabled() {
        return parallelExecutionEnabled;
    }


    public Logger getLogger() {
        ensureInitialized();
        return logger;
    }

    public IQueryRuntimeContext getRuntimeContext() {
        ensureInitialized();
        return runtimeContext;
    }

    public ReteRecipeCompiler getCompiler() {
        ensureInitialized();
       return compiler;
    }

    // /**
    // * For internal use only: logs exceptions occurring during term evaluation inside the RETE net.
    // * @param e
    // */
    // public void logEvaluatorException(Throwable e) {
    // try {
    // caughtExceptions.put(e);
    // } catch (InterruptedException e1) {
    // logEvaluatorException(e);
    // }
    // }
    // /**
    // * Polls the exceptions caught and logged during term evaluation by this RETE engine.
    // * Recommended usage: iterate polling until null is returned.
    // *
    // * @return the next caught exception, or null if there are no more.
    // */
    // public Throwable getNextLoggedEvaluatorException() {
    // return caughtExceptions.poll();
    // }

    void ensureInitialized() {
        if (disposedOrUninitialized)
            throw new IllegalStateException("Trying to use a Rete engine that has been disposed or has not yet been initialized.");

    }

    @Override
    public IQueryResultProvider getResultProvider(PQuery query)  {
        return accessMatcher(query);
    }

    /**
     * @since 1.4
     */
    @Override
    public IQueryResultProvider getResultProvider(PQuery query, QueryEvaluationHint hints) {
        hintConfigurator.storeHint(query, hints);
        return accessMatcher(query);
    }

    @Override
    public IQueryResultProvider peekExistingResultProvider(PQuery query) {
        ensureInitialized();
        return matchers.get(query);
    }

    @Override
    public void dispose() {
        killEngine();
    }

    @Override
    public boolean isCaching() {
        return true;
    }

    /**
     * @since 1.5
     * @noreference Internal API, subject to change
     */
    public IQueryBackendHintProvider getHintConfiguration() {
        return hintConfigurator;
    }

    @Override
    public IQueryBackendFactory getFactory() {
        return ReteBackendFactory.INSTANCE;
    }

}
