/*******************************************************************************
 * Copyright (c) 2010-2015, Bergmann Gabor, Istvan Rath and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.interpreter.rete.boundary;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import tools.refinery.interpreter.matchers.context.IInputKey;
import tools.refinery.interpreter.matchers.context.IQueryBackendContext;
import tools.refinery.interpreter.matchers.context.IQueryRuntimeContext;
import tools.refinery.interpreter.matchers.context.IQueryRuntimeContextListener;
import tools.refinery.interpreter.matchers.tuple.Tuple;
import tools.refinery.interpreter.matchers.tuple.TupleMask;
import tools.refinery.interpreter.matchers.tuple.Tuples;
import tools.refinery.interpreter.matchers.util.Direction;
import tools.refinery.interpreter.matchers.util.timeline.Timeline;
import tools.refinery.interpreter.rete.matcher.ReteEngine;
import tools.refinery.interpreter.rete.network.Network;
import tools.refinery.interpreter.rete.network.Receiver;
import tools.refinery.interpreter.rete.network.ReteContainer;
import tools.refinery.interpreter.rete.network.StandardNode;
import tools.refinery.interpreter.rete.network.Supplier;
import tools.refinery.interpreter.rete.network.communication.Timestamp;
import tools.refinery.interpreter.rete.network.mailbox.Mailbox;
import tools.refinery.interpreter.rete.network.mailbox.timeless.BehaviorChangingMailbox;
import tools.refinery.interpreter.rete.network.mailbox.timely.TimelyMailbox;
import tools.refinery.interpreter.rete.remote.Address;

/**
 * An input node representing an enumerable extensional input relation and receiving external updates.
 *
 * <p>
 * Contains those tuples that are in the extensional relation identified by the input key, and also conform to the
 * global seed (if any).
 *
 * @author Bergmann Gabor
 *
 */
public class ExternalInputEnumeratorNode extends StandardNode
        implements Disconnectable, Receiver, IQueryRuntimeContextListener {

    private IQueryRuntimeContext context = null;
    private IInputKey inputKey;
    private Tuple globalSeed;
    private InputConnector inputConnector;
    private Network network;
    private Address<? extends Receiver> myAddress;
    private boolean parallelExecutionEnabled;
    /**
     * @since 1.6
     */
    protected final Mailbox mailbox;
    private final IQueryBackendContext qBackendContext;

    public ExternalInputEnumeratorNode(ReteContainer reteContainer) {
        super(reteContainer);
        myAddress = Address.of(this);
        network = reteContainer.getNetwork();
        inputConnector = network.getInputConnector();
        qBackendContext = network.getEngine().getBackendContext();
        mailbox = instantiateMailbox();
        reteContainer.registerClearable(mailbox);
    }

    /**
     * Instantiates the {@link Mailbox} of this receiver. Subclasses may override this method to provide their own
     * mailbox implementation.
     *
     * @return the mailbox
     * @since 2.0
     */
    protected Mailbox instantiateMailbox() {
        if (this.reteContainer.isTimelyEvaluation()) {
            return new TimelyMailbox(this, this.reteContainer);
        } else {
            return new BehaviorChangingMailbox(this, this.reteContainer);
        }
    }

    @Override
    public Mailbox getMailbox() {
        return this.mailbox;
    }

    public void connectThroughContext(ReteEngine engine, IInputKey inputKey, Tuple globalSeed) {
        this.inputKey = inputKey;
        this.globalSeed = globalSeed;
        setTag(inputKey);

        final IQueryRuntimeContext context = engine.getRuntimeContext();
        if (!context.getMetaContext().isEnumerable(inputKey))
            throw new IllegalArgumentException(this.getClass().getSimpleName()
                    + " only applicable for enumerable input keys; received instead " + inputKey);

        this.context = context;
        this.parallelExecutionEnabled = engine.isParallelExecutionEnabled();

        engine.addDisconnectable(this);
        context.addUpdateListener(inputKey, globalSeed, this);
    }

    @Override
    public void disconnect() {
        if (context != null) { // if connected
            context.removeUpdateListener(inputKey, globalSeed, this);
            context = null;
        }
    }

    /**
     * @since 2.2
     */
    protected Iterable<Tuple> getTuplesInternal() {
        Iterable<Tuple> tuples = null;

        if (context != null) { // if connected
            if (globalSeed == null) {
                tuples = context.enumerateTuples(inputKey, TupleMask.empty(inputKey.getArity()),
                        Tuples.staticArityFlatTupleOf());
            } else {
                final TupleMask mask = TupleMask.fromNonNullIndices(globalSeed);
                tuples = context.enumerateTuples(inputKey, mask, mask.transform(globalSeed));
            }
        }

        return tuples;
    }

    @Override
    public void pullInto(final Collection<Tuple> collector, final boolean flush) {
        final Iterable<Tuple> tuples = getTuplesInternal();
        if (tuples != null) {
            for (final Tuple tuple : tuples) {
                collector.add(tuple);
            }
        }
    }

    @Override
    public void pullIntoWithTimeline(final Map<Tuple, Timeline<Timestamp>> collector, final boolean flush) {
        final Iterable<Tuple> tuples = getTuplesInternal();
        if (tuples != null) {
            for (final Tuple tuple : tuples) {
                collector.put(tuple, Timestamp.INSERT_AT_ZERO_TIMELINE);
            }
        }
    }

    /* Update from runtime context */
    @Override
    public void update(IInputKey key, Tuple update, boolean isInsertion) {
        if (parallelExecutionEnabled) {
            // send back to myself as an official external update, and then propagate it transparently
            network.sendExternalUpdate(myAddress, direction(isInsertion), update);
        } else {
            if (qBackendContext.areUpdatesDelayed()) {
                // post the update into the mailbox of the node
                mailbox.postMessage(direction(isInsertion), update, Timestamp.ZERO);
            } else {
                // just propagate the input
                update(direction(isInsertion), update, Timestamp.ZERO);
            }
            // if the the update method is called from within a delayed execution,
            // the following invocation will be a no-op
            network.waitForReteTermination();
        }
    }

    private static Direction direction(boolean isInsertion) {
        return isInsertion ? Direction.INSERT : Direction.DELETE;
    }

    /* Self-addressed from network */
    @Override
    public void update(Direction direction, Tuple updateElement, Timestamp timestamp) {
        propagateUpdate(direction, updateElement, timestamp);
    }

    @Override
    public void appendParent(Supplier supplier) {
        throw new UnsupportedOperationException("Input nodes can't have parents");
    }

    @Override
    public void removeParent(Supplier supplier) {
        throw new UnsupportedOperationException("Input nodes can't have parents");
    }

    @Override
    public Collection<Supplier> getParents() {
        return Collections.emptySet();
    }

    public IInputKey getInputKey() {
        return inputKey;
    }

    public Tuple getGlobalSeed() {
        return globalSeed;
    }

}
