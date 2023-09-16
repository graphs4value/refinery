/*******************************************************************************
 * Copyright (c) 2004-2009 Gabor Bergmann and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package tools.refinery.interpreter.rete.index;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;

import tools.refinery.interpreter.rete.matcher.TimelyConfiguration;
import tools.refinery.interpreter.rete.network.NetworkStructureChangeSensitiveNode;
import tools.refinery.interpreter.rete.network.Receiver;
import tools.refinery.interpreter.rete.network.ReteContainer;
import tools.refinery.interpreter.rete.network.Supplier;
import tools.refinery.interpreter.rete.network.communication.CommunicationGroup;
import tools.refinery.interpreter.rete.network.communication.Timestamp;
import tools.refinery.interpreter.rete.network.communication.timely.ResumableNode;
import tools.refinery.interpreter.rete.network.mailbox.Mailbox;
import tools.refinery.interpreter.rete.network.mailbox.timeless.BehaviorChangingMailbox;
import tools.refinery.interpreter.rete.network.mailbox.timely.TimelyMailbox;
import tools.refinery.interpreter.matchers.memories.MaskedTupleMemory;
import tools.refinery.interpreter.matchers.tuple.Tuple;
import tools.refinery.interpreter.matchers.tuple.TupleMask;
import tools.refinery.interpreter.matchers.util.CollectionsFactory;
import tools.refinery.interpreter.matchers.util.CollectionsFactory.MemoryType;
import tools.refinery.interpreter.matchers.util.Direction;
import tools.refinery.interpreter.matchers.util.Signed;
import tools.refinery.interpreter.matchers.util.timeline.Diff;

/**
 * @author Gabor Bergmann
 * @author Tamas Szabo
 */
public abstract class IndexerWithMemory extends StandardIndexer
        implements Receiver, NetworkStructureChangeSensitiveNode, ResumableNode {

    protected MaskedTupleMemory<Timestamp> memory;

    /**
     * @since 2.3
     */
    protected NetworkStructureChangeSensitiveLogic logic;

    /**
     * @since 1.6
     */
    protected final Mailbox mailbox;

    /**
     * @since 2.4
     */
    protected CommunicationGroup group;

    public IndexerWithMemory(final ReteContainer reteContainer, final TupleMask mask) {
        super(reteContainer, mask);
        final boolean isTimely = reteContainer.isTimelyEvaluation()
                && reteContainer.getCommunicationTracker().isInRecursiveGroup(this);
        memory = MaskedTupleMemory.create(mask, MemoryType.SETS, this, isTimely, isTimely && reteContainer
                .getTimelyConfiguration().getTimelineRepresentation() == TimelyConfiguration.TimelineRepresentation.FAITHFUL);
        reteContainer.registerClearable(memory);
        mailbox = instantiateMailbox();
        reteContainer.registerClearable(mailbox);
        this.logic = createLogic();
    }

    @Override
    public CommunicationGroup getCurrentGroup() {
        return this.group;
    }

    @Override
    public void setCurrentGroup(final CommunicationGroup group) {
        this.group = group;
    }

    @Override
    public void networkStructureChanged() {
        super.networkStructureChanged();
        final boolean wasTimely = this.memory.isTimely();
        final boolean isTimely = this.reteContainer.isTimelyEvaluation()
                && this.reteContainer.getCommunicationTracker().isInRecursiveGroup(this);
        if (wasTimely != isTimely) {
            final MaskedTupleMemory<Timestamp> newMemory = MaskedTupleMemory.create(mask, MemoryType.SETS, this,
                    isTimely, isTimely && reteContainer.getTimelyConfiguration()
                            .getTimelineRepresentation() == TimelyConfiguration.TimelineRepresentation.FAITHFUL);
            newMemory.initializeWith(this.memory, Timestamp.ZERO);
            memory.clear();
            memory = newMemory;
        }
        this.logic = createLogic();
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

    /**
     * @since 2.0
     */
    public MaskedTupleMemory<Timestamp> getMemory() {
        return memory;
    }

    @Override
    public void update(final Direction direction, final Tuple updateElement, final Timestamp timestamp) {
        this.logic.update(direction, updateElement, timestamp);
    }

    /**
     * Refined version of update
     *
     * @since 2.4
     */
    protected abstract void update(final Direction direction, final Tuple updateElement, final Tuple signature,
            final boolean change, final Timestamp timestamp);

    @Override
    public void appendParent(final Supplier supplier) {
        if (parent == null) {
            parent = supplier;
        } else {
            throw new UnsupportedOperationException("Illegal RETE edge: " + this + " already has a parent (" + parent
                    + ") and cannot connect to additional parent (" + supplier + "). ");
        }
    }

    @Override
    public void removeParent(final Supplier supplier) {
        if (parent == supplier) {
            parent = null;
        } else {
            throw new IllegalArgumentException(
                    "Illegal RETE edge removal: the parent of " + this + " is not " + supplier);
        }
    }

    /**
     * @since 2.4
     */
    @Override
    public Collection<Supplier> getParents() {
        return Collections.singleton(parent);
    }

    /**
     * @since 2.4
     */
    @Override
    public void resumeAt(final Timestamp timestamp) {
        this.logic.resumeAt(timestamp);
    }

    /**
     * @since 2.4
     */
    @Override
    public Timestamp getResumableTimestamp() {
        return this.memory.getResumableTimestamp();
    }

    /**
     * @since 2.3
     */
    protected static abstract class NetworkStructureChangeSensitiveLogic {

        /**
         * @since 2.4
         */
        public abstract void update(final Direction direction, final Tuple updateElement, final Timestamp timestamp);

        /**
         * @since 2.4
         */
        public abstract void resumeAt(final Timestamp timestamp);

    }

    /**
     * @since 2.3
     */
    protected NetworkStructureChangeSensitiveLogic createLogic() {
        if (this.reteContainer.isTimelyEvaluation()
                && this.reteContainer.getCommunicationTracker().isInRecursiveGroup(this)) {
            return TIMELY;
        } else {
            return TIMELESS;
        }
    }

    private final NetworkStructureChangeSensitiveLogic TIMELY = new NetworkStructureChangeSensitiveLogic() {

        @Override
        public void resumeAt(final Timestamp timestamp) {
            final Iterable<Tuple> signatures = memory.getResumableSignatures();

            final Map<Tuple, Boolean> wasPresent = CollectionsFactory.createMap();
            for (final Tuple signature : signatures) {
                wasPresent.put(signature, memory.isPresentAtInfinity(signature));
            }

            final Map<Tuple, Map<Tuple, Diff<Timestamp>>> signatureMap = memory.resumeAt(timestamp);

            for (final Entry<Tuple, Map<Tuple, Diff<Timestamp>>> outerEntry : signatureMap.entrySet()) {
                final Tuple signature = outerEntry.getKey();
                final Map<Tuple, Diff<Timestamp>> diffMap = outerEntry.getValue();
                final boolean isPresent = memory.isPresentAtInfinity(signature);
                // only send out a potential true value the first time for a given signature, then set it to false
                boolean change = wasPresent.get(signature) ^ isPresent;

                for (final Entry<Tuple, Diff<Timestamp>> innerEntry : diffMap.entrySet()) {
                    final Tuple tuple = innerEntry.getKey();
                    final Diff<Timestamp> diffs = innerEntry.getValue();
                    for (final Signed<Timestamp> signed : diffs) {
                        IndexerWithMemory.this.update(signed.getDirection(), tuple, signature, change,
                                signed.getPayload());
                    }
                    // change is a signature-wise flag, so it is ok to "try" to signal it for the first tuple only
                    change = false;
                }
            }

            final Timestamp nextTimestamp = memory.getResumableTimestamp();
            if (nextTimestamp != null) {
                group.notifyHasMessage(mailbox, nextTimestamp);
            }
        }

        @Override
        public void update(final Direction direction, final Tuple update, final Timestamp timestamp) {
            final Tuple signature = mask.transform(update);
            final boolean wasPresent = memory.isPresentAtInfinity(signature);
            final Diff<Timestamp> resultDiff = direction == Direction.INSERT
                    ? memory.addWithTimestamp(update, signature, timestamp)
                    : memory.removeWithTimestamp(update, signature, timestamp);
            final boolean isPresent = memory.isPresentAtInfinity(signature);
            final boolean change = wasPresent ^ isPresent;
            for (final Signed<Timestamp> signed : resultDiff) {
                IndexerWithMemory.this.update(signed.getDirection(), update, signature, change, signed.getPayload());
            }
        }

    };

    private final NetworkStructureChangeSensitiveLogic TIMELESS = new NetworkStructureChangeSensitiveLogic() {

        @Override
        public void update(final Direction direction, final Tuple update, final Timestamp timestamp) {
            final Tuple signature = mask.transform(update);
            final boolean change = direction == Direction.INSERT ? memory.add(update, signature)
                    : memory.remove(update, signature);
            IndexerWithMemory.this.update(direction, update, signature, change, timestamp);
        }

        @Override
        public void resumeAt(final Timestamp timestamp) {
            // there is nothing to resume in the timeless case because we do not even care about timestamps
        }

    };

}
