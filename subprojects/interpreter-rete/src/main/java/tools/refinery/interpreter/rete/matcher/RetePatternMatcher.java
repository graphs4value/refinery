/*******************************************************************************
 * Copyright (c) 2004-2008 Gabor Bergmann and Daniel Varro
 * Copyright (c) 2023 The Refinery Authors <https://refinery.tools>
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package tools.refinery.interpreter.rete.matcher;

import tools.refinery.interpreter.rete.network.Node;
import tools.refinery.interpreter.rete.network.ProductionNode;
import tools.refinery.interpreter.rete.network.Receiver;
import tools.refinery.interpreter.matchers.backend.IQueryBackend;
import tools.refinery.interpreter.matchers.backend.IQueryResultProvider;
import tools.refinery.interpreter.matchers.backend.IUpdateable;
import tools.refinery.interpreter.matchers.context.IQueryRuntimeContext;
import tools.refinery.interpreter.matchers.tuple.ITuple;
import tools.refinery.interpreter.matchers.tuple.Tuple;
import tools.refinery.interpreter.matchers.tuple.TupleMask;
import tools.refinery.interpreter.matchers.tuple.Tuples;
import tools.refinery.interpreter.matchers.util.Accuracy;
import tools.refinery.interpreter.matchers.util.CollectionsFactory;
import tools.refinery.interpreter.rete.index.Indexer;
import tools.refinery.interpreter.rete.index.IterableIndexer;
import tools.refinery.interpreter.rete.remote.Address;
import tools.refinery.interpreter.rete.single.CallbackNode;
import tools.refinery.interpreter.rete.single.TransformerNode;
import tools.refinery.interpreter.rete.traceability.RecipeTraceInfo;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * @author Gabor Bergmann
 *
 */
public class RetePatternMatcher extends TransformerNode implements IQueryResultProvider {

    protected ReteEngine engine;
    protected IQueryRuntimeContext context;
    protected ProductionNode productionNode;
    protected RecipeTraceInfo productionNodeTrace;
    protected Map<String, Integer> posMapping;
    protected Map<Object, Receiver> taggedChildren = CollectionsFactory.createMap();
    protected boolean connected = false; // is rete-wise connected to the
                                         // production node?

    /**
     * @param productionNode
     *            a production node that matches this pattern without any parameter bindings
     * @pre: Production must be local to the head container
     */
    public RetePatternMatcher(ReteEngine engine, RecipeTraceInfo productionNodeTrace) {
        super(engine.getReteNet().getHeadContainer());
        this.engine = engine;
        this.context = engine.getRuntimeContext();
        this.productionNodeTrace = productionNodeTrace;
        final Address<? extends Node> productionAddress = reteContainer.getProvisioner()
                .getOrCreateNodeByRecipe(productionNodeTrace);
        if (!reteContainer.isLocal(productionAddress))
            throw new IllegalArgumentException("@pre: Production must be local to the head container");
        this.productionNode = (ProductionNode) reteContainer.resolveLocal(productionAddress);
        this.posMapping = this.productionNode.getPosMapping();
        this.reteContainer.getCommunicationTracker().registerDependency(this.productionNode, this);
    }

    /**
     * @since 1.6
     */
    public ProductionNode getProductionNode() {
        return productionNode;
    }

    /**
     * @since 2.0
     */
    public Stream<Tuple> matchAll(Object[] inputMapping, boolean[] fixed) {
        // retrieving the projection
        TupleMask mask = TupleMask.fromKeepIndicators(fixed);
        Tuple inputSignature = mask.transform(Tuples.flatTupleOf(inputMapping));

        return matchAll(mask, inputSignature);

    }

    /**
     * @since 2.0
     */
    public Stream<Tuple> matchAll(TupleMask mask, ITuple inputSignature) {
        AllMatchFetcher fetcher = new AllMatchFetcher(engine.accessProjection(productionNodeTrace, mask),
                context.wrapTuple(inputSignature.toImmutable()));
        engine.reteNet.waitForReteTermination(fetcher);
        return fetcher.getMatches();

    }

    /**
     * @since 2.0
     */
    public Optional<Tuple> matchOne(Object[] inputMapping, boolean[] fixed) {
        // retrieving the projection
        TupleMask mask = TupleMask.fromKeepIndicators(fixed);
        Tuple inputSignature = mask.transform(Tuples.flatTupleOf(inputMapping));

        return matchOne(mask, inputSignature);
    }

    /**
     * @since 2.0
     */
    public Optional<Tuple> matchOne(TupleMask mask, ITuple inputSignature) {
        SingleMatchFetcher fetcher = new SingleMatchFetcher(engine.accessProjection(productionNodeTrace, mask),
                context.wrapTuple(inputSignature.toImmutable()));
        engine.reteNet.waitForReteTermination(fetcher);
        return Optional.ofNullable(fetcher.getMatch());
    }

    /**
     * Counts the number of occurrences of the pattern that match inputMapping on positions where fixed is true.
     *
     * @return the number of occurrences
     */
    public int count(Object[] inputMapping, boolean[] fixed) {
        TupleMask mask = TupleMask.fromKeepIndicators(fixed);
        Tuple inputSignature = mask.transform(Tuples.flatTupleOf(inputMapping));

        return count(mask, inputSignature);
    }

    /**
     * Counts the number of occurrences of the pattern that match inputMapping on positions where fixed is true.
     *
     * @return the number of occurrences
     * @since 1.7
     */
    public int count(TupleMask mask, ITuple inputSignature) {
        CountFetcher fetcher = new CountFetcher(engine.accessProjection(productionNodeTrace, mask),
                context.wrapTuple(inputSignature.toImmutable()));
        engine.reteNet.waitForReteTermination(fetcher);

        return fetcher.getCount();
    }

    /**
     * Counts the number of distinct tuples attainable from the match set by projecting match tuples according to the given mask.
     *
     *
     * @return the size of the projection
     * @since 2.1
     */
    public int projectionSize(TupleMask groupMask) {
        ProjectionSizeFetcher fetcher = new ProjectionSizeFetcher(
                (IterableIndexer) engine.accessProjection(productionNodeTrace, groupMask));
        engine.reteNet.waitForReteTermination(fetcher);

        return fetcher.getSize();
    }

    /**
     * Connects a new external receiver that will receive update notifications from now on. The receiver will
     * practically connect to the production node, the added value is unwrapping the updates for external use.
     *
     * @param synchronize
     *            if true, the contents of the production node will be inserted into the receiver after the connection
     *            is established.
     */
    public synchronized void connect(Receiver receiver, boolean synchronize) {
        if (!connected) { // connect to the production node as a RETE-child
            reteContainer.connect(productionNode, this);
            connected = true;
        }
        if (synchronize)
            reteContainer.connectAndSynchronize(this, receiver);
        else
            reteContainer.connect(this, receiver);
    }

    /**
     * Connects a new external receiver that will receive update notifications from now on. The receiver will
     * practically connect to the production node, the added value is unwrapping the updates for external use.
     *
     * The external receiver will be disconnectable later based on its tag.
     *
     * @param tag
     *            an identifier to recognize the child node by.
     *
     * @param synchronize
     *            if true, the contents of the production node will be inserted into the receiver after the connection
     *            is established.
     *
     */
    public synchronized void connect(Receiver receiver, Object tag, boolean synchronize) {
        taggedChildren.put(tag, receiver);
        connect(receiver, synchronize);
    }

    /**
     * Disconnects a child node.
     */
    public synchronized void disconnect(Receiver receiver) {
        reteContainer.disconnect(this, receiver);
    }

    /**
     * Disconnects the child node that was connected by specifying the given tag.
     *
     * @return if a child node was found registered with this tag.
     */
    public synchronized boolean disconnectByTag(Object tag) {
        final Receiver receiver = taggedChildren.remove(tag);
        final boolean found = receiver != null;
        if (found)
            disconnect(receiver);
        return found;
    }

    @Override
    protected Tuple transform(Tuple input) {
        return context.unwrapTuple(input);
    }

    abstract class AbstractMatchFetcher implements Runnable {
        Indexer indexer;
        Tuple signature;

        public AbstractMatchFetcher(Indexer indexer, Tuple signature) {
            super();
            this.indexer = indexer;
            this.signature = signature;
        }

        @Override
        public void run() {
            fetch(indexer.get(signature));
        }

        protected abstract void fetch(Collection<Tuple> matches);

    }

    class AllMatchFetcher extends AbstractMatchFetcher {

        public AllMatchFetcher(Indexer indexer, Tuple signature) {
            super(indexer, signature);
        }

        Stream<Tuple> matches = null;

        public Stream<Tuple> getMatches() {
            return matches;
        }

        @Override
        protected void fetch(Collection<Tuple> matches) {
            if (matches == null)
                this.matches = Stream.of();
            else {
                this.matches = matches.stream().map(context::unwrapTuple);
            }

        }

    }

    class SingleMatchFetcher extends AbstractMatchFetcher {

        public SingleMatchFetcher(Indexer indexer, Tuple signature) {
            super(indexer, signature);
        }

        Tuple match = null;

        public Tuple getMatch() {
            return match;
        }

        @Override
        protected void fetch(Collection<Tuple> matches) {
            if (matches != null && !matches.isEmpty())
                match = context.unwrapTuple(matches.iterator().next());
        }

        // public void run() {
        // Collection<Tuple> unscopedMatches = indexer.get(signature);
        //
        // // checking scopes
        // if (unscopedMatches != null) {
        // for (Tuple um : /* productionNode */unscopedMatches) {
        // match = inputConnector.unwrapTuple(um);
        // return;
        //
        // // Tuple ps = inputConnector.unwrapTuple(um);
        // // boolean ok = true;
        // // if (!ignoreScope) for (int k = 0; (k < ps.getSize()) && ok; k++) {
        // // if (pcs[k].getParameterMode() == ParameterMode.INPUT) {
        // // // ok = ok && (inputMapping[k]==ps.elements[k]);
        // // // should now be true
        // // } else // ParameterMode.OUTPUT
        // // {
        // // IEntity scopeParent = (IEntity) pcs[k].getParameterScope().getParent();
        // // Integer containmentMode = pcs[k].getParameterScope().getContainmentMode();
        // // if (containmentMode == Scope.BELOW)
        // // ok = ok && ((IModelElement) ps.get(k)).isBelowNamespace(scopeParent);
        // // else
        // // /* case Scope.IN: */
        // // ok = ok && scopeParent.equals(((IModelElement) ps.get(k)).getNamespace());
        // // // note: getNamespace returns null instead of the
        // // // (imaginary) modelspace root entity for top level
        // // // elements;
        // // // this is not a problem here as Scope.IN implies
        // // // scopeParent != root.
        // //
        // // }
        // // }
        // //
        // // if (ok) {
        // // reteMatching = new ReteMatching(ps, posMapping);
        // // return;
        // // }
        // }
        // }
        //
        // }

    }

    class CountFetcher extends AbstractMatchFetcher {

        public CountFetcher(Indexer indexer, Tuple signature) {
            super(indexer, signature);
        }

        int count = 0;

        public int getCount() {
            return count;
        }

        @Override
        protected void fetch(Collection<Tuple> matches) {
            count = matches == null ? 0 : matches.size();
        }

    }

    class ProjectionSizeFetcher implements Runnable {
        IterableIndexer indexer;
        int size = 0;

        public ProjectionSizeFetcher(IterableIndexer indexer) {
            super();
            this.indexer = indexer;
        }

        @Override
        public void run() {
            size = indexer.getBucketCount();
        }

        public int getSize() {
            return size;
        }

    }

    private boolean[] notNull(Object[] parameters) {
        boolean[] notNull = new boolean[parameters.length];
        for (int i = 0; i < parameters.length; ++i)
            notNull[i] = parameters[i] != null;
        return notNull;
    }



    @Override
    public boolean hasMatch(Object[] parameters) {
        return countMatches(parameters) > 0;
    }

    @Override
    public boolean hasMatch(TupleMask parameterSeedMask, ITuple parameters) {
        return count(parameterSeedMask, parameters) > 0;
    }

    @Override
    public int countMatches(Object[] parameters) {
        return count(parameters, notNull(parameters));
    }

    @Override
    public int countMatches(TupleMask parameterSeedMask, ITuple parameters) {
        return count(parameterSeedMask, parameters);
    }


    @Override
    public Optional<Long> estimateCardinality(TupleMask groupMask, Accuracy requiredAccuracy) {
        return Optional.of((long)projectionSize(groupMask)); // always accurate
    }

    @Override
    public Optional<Tuple> getOneArbitraryMatch(Object[] parameters) {
        return matchOne(parameters, notNull(parameters));
    }

    @Override
    public Optional<Tuple> getOneArbitraryMatch(TupleMask parameterSeedMask, ITuple parameters) {
        return matchOne(parameterSeedMask, parameters);
    }

    @Override
    public Stream<Tuple> getAllMatches(Object[] parameters) {
        return matchAll(parameters, notNull(parameters));
    }

    @Override
    public Stream<Tuple> getAllMatches(TupleMask parameterSeedMask, ITuple parameters) {
        return matchAll(parameterSeedMask, parameters);
    }

    @Override
    public IQueryBackend getQueryBackend() {
        return engine;
    }

    @Override
    public void addUpdateListener(final IUpdateable listener, final Object listenerTag, boolean fireNow) {
        // As a listener is added as a delayed command, they should be executed to make sure everything is consistent on
        // return, see bug https://bugs.eclipse.org/bugs/show_bug.cgi?id=562369
        engine.constructionWrapper(() -> {
            final CallbackNode callbackNode = new CallbackNode(this.reteContainer, listener);
            connect(callbackNode, listenerTag, fireNow);
            return null;
        });
    }

    @Override
    public void removeUpdateListener(Object listenerTag) {
        engine.constructionWrapper(() -> {
            disconnectByTag(listenerTag);
            return null;
        });
    }

	public Indexer getInternalIndexer(TupleMask mask) {
		return engine.accessProjection(productionNodeTrace, mask);
	}
}
