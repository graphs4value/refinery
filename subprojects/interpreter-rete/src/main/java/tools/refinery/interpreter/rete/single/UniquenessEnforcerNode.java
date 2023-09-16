/*******************************************************************************
 * Copyright (c) 2004-2008 Gabor Bergmann, Tamas Szabo and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package tools.refinery.interpreter.rete.single;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import tools.refinery.interpreter.rete.network.PosetAwareReceiver;
import tools.refinery.interpreter.rete.network.RederivableNode;
import tools.refinery.interpreter.rete.network.ReteContainer;
import tools.refinery.interpreter.rete.network.communication.CommunicationGroup;
import tools.refinery.interpreter.rete.network.communication.Timestamp;
import tools.refinery.interpreter.rete.network.communication.timeless.RecursiveCommunicationGroup;
import tools.refinery.interpreter.rete.network.mailbox.Mailbox;
import tools.refinery.interpreter.rete.network.mailbox.timeless.BehaviorChangingMailbox;
import tools.refinery.interpreter.rete.network.mailbox.timeless.PosetAwareMailbox;
import tools.refinery.interpreter.matchers.context.IPosetComparator;
import tools.refinery.interpreter.matchers.tuple.Tuple;
import tools.refinery.interpreter.matchers.tuple.TupleMask;
import tools.refinery.interpreter.matchers.util.CollectionsFactory;
import tools.refinery.interpreter.matchers.util.Direction;
import tools.refinery.interpreter.matchers.util.IMultiset;
import tools.refinery.interpreter.matchers.util.timeline.Timeline;
import tools.refinery.interpreter.rete.index.MemoryIdentityIndexer;
import tools.refinery.interpreter.rete.index.MemoryNullIndexer;
import tools.refinery.interpreter.rete.index.ProjectionIndexer;

/**
 * Timeless uniqueness enforcer node implementation.
 * <p>
 * The node is capable of operating in the delete and re-derive mode. In this mode, it is also possible to equip the
 * node with an {@link IPosetComparator} to identify monotone changes; thus, ensuring that a fix-point can be reached
 * during the evaluation.
 *
 * @author Gabor Bergmann
 * @author Tamas Szabo
 * @noinstantiate This class is not intended to be instantiated by clients.
 * @noextend This class is not intended to be subclassed by clients.
 */
public class UniquenessEnforcerNode extends AbstractUniquenessEnforcerNode
        implements RederivableNode, PosetAwareReceiver {

    protected IMultiset<Tuple> memory;
    /**
     * @since 1.6
     */
    protected IMultiset<Tuple> rederivableMemory;
    /**
     * @since 1.6
     */
    protected boolean deleteRederiveEvaluation;

    /**
     * @since 1.7
     */
    protected CommunicationGroup currentGroup;

    public UniquenessEnforcerNode(final ReteContainer reteContainer, final int tupleWidth) {
        this(reteContainer, tupleWidth, false);
    }

    /**
     * OPTIONAL ELEMENT - ONLY PRESENT IF MONOTONICITY INFO WAS AVAILABLE
     *
     * @since 1.6
     */
    protected final TupleMask coreMask;
    /**
     * OPTIONAL ELEMENTS - ONLY PRESENT IF MONOTONICITY INFO WAS AVAILABLE
     *
     * @since 1.6
     */
    protected final TupleMask posetMask;
    /**
     * OPTIONAL ELEMENTS - ONLY PRESENT IF MONOTONICITY INFO WAS AVAILABLE
     *
     * @since 1.6
     */
    protected final IPosetComparator posetComparator;

    /**
     * @since 1.6
     */
    public UniquenessEnforcerNode(final ReteContainer reteContainer, final int tupleWidth,
            final boolean deleteRederiveEvaluation) {
        this(reteContainer, tupleWidth, deleteRederiveEvaluation, null, null, null);
    }

    /**
     * @since 1.6
     */
    public UniquenessEnforcerNode(final ReteContainer reteContainer, final int tupleWidth,
            final boolean deleteRederiveEvaluation, final TupleMask coreMask, final TupleMask posetMask,
            final IPosetComparator posetComparator) {
        super(reteContainer, tupleWidth);
        this.memory = CollectionsFactory.createMultiset();
        this.rederivableMemory = CollectionsFactory.createMultiset();
        reteContainer.registerClearable(this.memory);
        reteContainer.registerClearable(this.rederivableMemory);
        this.deleteRederiveEvaluation = deleteRederiveEvaluation;
        this.coreMask = coreMask;
        this.posetMask = posetMask;
        this.posetComparator = posetComparator;
        this.mailbox = instantiateMailbox();
        reteContainer.registerClearable(this.mailbox);
    }

    @Override
    public void pullInto(final Collection<Tuple> collector, final boolean flush) {
        for (final Tuple tuple : this.memory.distinctValues()) {
            collector.add(tuple);
        }
    }

    /**
     * @since 2.8
     */
    @Override
    public Set<Tuple> getTuples() {
        return this.memory.distinctValues();
    }

    @Override
    public boolean isInDRedMode() {
        return this.deleteRederiveEvaluation;
    }

    @Override
    public TupleMask getCoreMask() {
        return coreMask;
    }

    @Override
    public TupleMask getPosetMask() {
        return posetMask;
    }

    @Override
    public IPosetComparator getPosetComparator() {
        return posetComparator;
    }

    @Override
    public void pullIntoWithTimeline(final Map<Tuple, Timeline<Timestamp>> collector, final boolean flush) {
        throw new UnsupportedOperationException("Use the timely version of this node!");
    }

    /**
     * @since 2.0
     */
    protected Mailbox instantiateMailbox() {
        if (coreMask != null && posetMask != null && posetComparator != null) {
            return new PosetAwareMailbox(this, this.reteContainer);
        } else {
            return new BehaviorChangingMailbox(this, this.reteContainer);
        }
    }

    @Override
    public void update(final Direction direction, final Tuple update, final Timestamp timestamp) {
        updateWithPosetInfo(direction, update, false);
    }

    @Override
    public void updateWithPosetInfo(final Direction direction, final Tuple update, final boolean monotone) {
        if (this.deleteRederiveEvaluation) {
            if (updateWithDeleteAndRederive(direction, update, monotone)) {
                propagate(direction, update, Timestamp.ZERO);
            }
        } else {
            if (updateDefault(direction, update)) {
                propagate(direction, update, Timestamp.ZERO);
            }
        }
    }

    /**
     * @since 2.4
     */
    protected boolean updateWithDeleteAndRederive(final Direction direction, final Tuple update,
            final boolean monotone) {
        boolean propagate = false;

        final int memoryCount = memory.getCount(update);
        final int rederivableCount = rederivableMemory.getCount(update);

        if (direction == Direction.INSERT) {
            // INSERT
            if (rederivableCount != 0) {
                // the tuple is in the re-derivable memory
                rederivableMemory.addOne(update);
                if (rederivableMemory.isEmpty()) {
                    // there is nothing left to be re-derived
                    // this can happen if the INSERT cancelled out a DELETE
                    ((RecursiveCommunicationGroup) currentGroup).removeRederivable(this);
                }
            } else {
                // the tuple is in the main memory
                propagate = memory.addOne(update);
            }
        } else {
            // DELETE
            if (rederivableCount != 0) {
                // the tuple is in the re-derivable memory
                if (memoryCount != 0) {
                    issueError("[INTERNAL ERROR] Inconsistent state for " + update
                            + " because it is present both in the main and re-derivable memory in the UniquenessEnforcerNode "
                            + this + " for pattern(s) " + getTraceInfoPatternsEnumerated(), null);
                }

                try {
                    rederivableMemory.removeOne(update);
                } catch (final IllegalStateException ex) {
                    issueError(
                            "[INTERNAL ERROR] Duplicate deletion of " + update + " was detected in UniquenessEnforcer "
                                    + this + " for pattern(s) " + getTraceInfoPatternsEnumerated(),
                            ex);
                }
                if (rederivableMemory.isEmpty()) {
                    // there is nothing left to be re-derived
                    ((RecursiveCommunicationGroup) currentGroup).removeRederivable(this);
                }
            } else {
                // the tuple is in the main memory
                if (monotone) {
                    propagate = memory.removeOne(update);
                } else {
                    final int count = memoryCount - 1;
                    if (count > 0) {
                        if (rederivableMemory.isEmpty()) {
                            // there is now something to be re-derived
                            ((RecursiveCommunicationGroup) currentGroup).addRederivable(this);
                        }
                        rederivableMemory.addPositive(update, count);
                    }
                    memory.clearAllOf(update);
                    propagate = true;
                }
            }
        }

        return propagate;
    }

    /**
     * @since 2.4
     */
    protected boolean updateDefault(final Direction direction, final Tuple update) {
        boolean propagate = false;
        if (direction == Direction.INSERT) {
            // INSERT
            propagate = memory.addOne(update);
        } else {
            // DELETE
            try {
                propagate = memory.removeOne(update);
            } catch (final IllegalStateException ex) {
                propagate = false;
                issueError("[INTERNAL ERROR] Duplicate deletion of " + update + " was detected in "
                        + this.getClass().getName() + " " + this + " for pattern(s) "
                        + getTraceInfoPatternsEnumerated(), ex);
            }
        }
        return propagate;
    }

    /**
     * @since 1.6
     */
    @Override
    public void rederiveOne() {
        final Tuple update = rederivableMemory.iterator().next();
        final int count = rederivableMemory.getCount(update);
        rederivableMemory.clearAllOf(update);
        memory.addPositive(update, count);
        // if there is no other re-derivable tuple, then unregister the node itself
        if (this.rederivableMemory.isEmpty()) {
            ((RecursiveCommunicationGroup) currentGroup).removeRederivable(this);
        }
        propagate(Direction.INSERT, update, Timestamp.ZERO);
    }

    @Override
    public ProjectionIndexer getNullIndexer() {
        if (this.memoryNullIndexer == null) {
            this.memoryNullIndexer = new MemoryNullIndexer(this.reteContainer, this.tupleWidth,
                    this.memory.distinctValues(), this, this, this.specializedListeners);
            this.getCommunicationTracker().registerDependency(this, this.memoryNullIndexer);
        }
        return this.memoryNullIndexer;
    }

    @Override
    public ProjectionIndexer getIdentityIndexer() {
        if (this.memoryIdentityIndexer == null) {
            this.memoryIdentityIndexer = new MemoryIdentityIndexer(this.reteContainer, this.tupleWidth,
                    this.memory.distinctValues(), this, this, this.specializedListeners);
            this.getCommunicationTracker().registerDependency(this, this.memoryIdentityIndexer);
        }
        return this.memoryIdentityIndexer;
    }

    @Override
    public CommunicationGroup getCurrentGroup() {
        return currentGroup;
    }

    @Override
    public void setCurrentGroup(final CommunicationGroup currentGroup) {
        this.currentGroup = currentGroup;
    }

}
