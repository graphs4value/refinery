/*******************************************************************************
 * Copyright (c) 2010-2016, Tamas Szabo, Istvan Rath and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.interpreter.rete.network.mailbox.timeless;

import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Set;

import tools.refinery.interpreter.rete.network.communication.MessageSelector;
import tools.refinery.interpreter.rete.network.communication.PhasedSelector;
import tools.refinery.interpreter.rete.network.communication.Timestamp;
import tools.refinery.interpreter.matchers.context.IPosetComparator;
import tools.refinery.interpreter.matchers.tuple.Tuple;
import tools.refinery.interpreter.matchers.tuple.TupleMask;
import tools.refinery.interpreter.matchers.util.CollectionsFactory;
import tools.refinery.interpreter.matchers.util.Direction;
import tools.refinery.interpreter.rete.network.PosetAwareReceiver;
import tools.refinery.interpreter.rete.network.ReteContainer;
import tools.refinery.interpreter.rete.network.indexer.GroupBasedMessageIndexer;

/**
 * A monotonicity aware mailbox implementation. The mailbox uses an {@link IPosetComparator} to identify if a pair of
 * REVOKE - INSERT updates represent a monotone change pair. The mailbox is used by {@link PosetAwareReceiver}s.
 *
 * @author Tamas Szabo
 * @since 2.0
 */
public class PosetAwareMailbox extends AbstractUpdateSplittingMailbox<GroupBasedMessageIndexer, PosetAwareReceiver> {

    protected final TupleMask groupMask;

    public PosetAwareMailbox(final PosetAwareReceiver receiver, final ReteContainer container) {
        super(receiver, container, () -> new GroupBasedMessageIndexer(receiver.getCoreMask()));
        this.groupMask = receiver.getCoreMask();
    }

    @Override
    public void postMessage(final Direction direction, final Tuple update, final Timestamp timestamp) {
        final GroupBasedMessageIndexer monotoneQueue = getActiveMonotoneQueue();
        final GroupBasedMessageIndexer antiMonotoneQueue = getActiveAntiMonotoneQueue();
        final boolean wasPresentAsMonotone = monotoneQueue.getCount(update) != 0;
        final boolean wasPresentAsAntiMonotone = antiMonotoneQueue.getCount(update) != 0;
        final TupleMask coreMask = this.receiver.getCoreMask();

        // it cannot happen that it was present in both
        assert !(wasPresentAsMonotone && wasPresentAsAntiMonotone);

        if (direction == Direction.INSERT) {
            if (wasPresentAsAntiMonotone) {
                // it was an anti-monotone one before
                antiMonotoneQueue.insert(update);
            } else {
                // it was a monotone one before or did not exist at all
                monotoneQueue.insert(update);

                // if it was not present in the monotone queue before, then
                // we need to check whether it makes REVOKE updates monotone
                if (!wasPresentAsMonotone) {
                    final Set<Tuple> counterParts = tryFindCounterPart(update, false, true);
                    for (final Tuple counterPart : counterParts) {
                        final int count = antiMonotoneQueue.getCount(counterPart);
                        assert count < 0;
                        antiMonotoneQueue.update(counterPart, -count);
                        monotoneQueue.update(counterPart, count);
                    }
                }
            }
        } else {
            if (wasPresentAsAntiMonotone) {
                // it was an anti-monotone one before
                antiMonotoneQueue.delete(update);
            } else if (wasPresentAsMonotone) {
                // it was a monotone one before
                monotoneQueue.delete(update);

                // and we need to check whether the monotone REVOKE updates
                // still have a reinforcing counterpart
                final Set<Tuple> candidates = new HashSet<Tuple>();
                final Tuple key = coreMask.transform(update);
                for (final Entry<Tuple, Integer> entry : monotoneQueue.getTuplesByGroup(key).entrySet()) {
                    if (entry.getValue() < 0) {
                        final Tuple candidate = entry.getKey();
                        final Set<Tuple> counterParts = tryFindCounterPart(candidate, true, false);
                        if (counterParts.isEmpty()) {
                            // all of them are gone
                            candidates.add(candidate);
                        }
                    }
                }

                // move the candidates from the monotone queue to the
                // anti-monotone queue because they do not have a
                // counterpart anymore
                for (final Tuple candidate : candidates) {
                    final int count = monotoneQueue.getCount(candidate);
                    assert count < 0;
                    monotoneQueue.update(candidate, -count);
                    antiMonotoneQueue.update(candidate, count);
                }
            } else {
                // it did not exist before
                final Set<Tuple> counterParts = tryFindCounterPart(update, true, false);
                if (counterParts.isEmpty()) {
                    // there is no tuple that would make this update monotone
                    antiMonotoneQueue.delete(update);
                } else {
                    // there is a reinforcing counterpart
                    monotoneQueue.delete(update);
                }
            }
        }

        if (antiMonotoneQueue.isEmpty()) {
            this.group.notifyLostAllMessages(this, PhasedSelector.ANTI_MONOTONE);
        } else {
            this.group.notifyHasMessage(this, PhasedSelector.ANTI_MONOTONE);
        }

        if (monotoneQueue.isEmpty()) {
            this.group.notifyLostAllMessages(this, PhasedSelector.MONOTONE);
        } else {
            this.group.notifyHasMessage(this, PhasedSelector.MONOTONE);
        }
    }

    protected Set<Tuple> tryFindCounterPart(final Tuple first, final boolean findPositiveCounterPart,
            final boolean findAllCounterParts) {
        final GroupBasedMessageIndexer monotoneQueue = getActiveMonotoneQueue();
        final GroupBasedMessageIndexer antiMonotoneQueue = getActiveAntiMonotoneQueue();
        final TupleMask coreMask = this.receiver.getCoreMask();
        final TupleMask posetMask = this.receiver.getPosetMask();
        final IPosetComparator posetComparator = this.receiver.getPosetComparator();
        final Set<Tuple> result = CollectionsFactory.createSet();
        final Tuple firstKey = coreMask.transform(first);
        final Tuple firstValue = posetMask.transform(first);

        if (findPositiveCounterPart) {
            for (final Entry<Tuple, Integer> entry : monotoneQueue.getTuplesByGroup(firstKey).entrySet()) {
                final Tuple secondValue = posetMask.transform(entry.getKey());
                if (entry.getValue() > 0 && posetComparator.isLessOrEqual(firstValue, secondValue)) {
                    result.add(entry.getKey());
                    if (!findAllCounterParts) {
                        return result;
                    }
                }
            }
        } else {
            for (final Entry<Tuple, Integer> entry : antiMonotoneQueue.getTuplesByGroup(firstKey).entrySet()) {
                final Tuple secondValue = posetMask.transform(entry.getKey());
                if (posetComparator.isLessOrEqual(secondValue, firstValue)) {
                    result.add(entry.getKey());
                    if (!findAllCounterParts) {
                        return result;
                    }
                }
            }
        }

        return result;
    }

    @Override
    public void deliverAll(final MessageSelector kind) {
        if (kind == PhasedSelector.ANTI_MONOTONE) {
            // use the buffer during delivering so that there is a clear
            // separation between the stages
            this.deliveringAntiMonotone = true;

            for (final Tuple group : this.antiMonotoneQueue.getGroups()) {
                for (final Entry<Tuple, Integer> entry : this.antiMonotoneQueue.getTuplesByGroup(group).entrySet()) {
                    final Tuple update = entry.getKey();
                    final int count = entry.getValue();
                    assert count < 0;
                    for (int i = 0; i < Math.abs(count); i++) {
                        this.receiver.updateWithPosetInfo(Direction.DELETE, update, false);
                    }
                }
            }

            this.deliveringAntiMonotone = false;
            swapAndClearAntiMonotone();
        } else if (kind == PhasedSelector.MONOTONE) {
            // use the buffer during delivering so that there is a clear
            // separation between the stages
            this.deliveringMonotone = true;

            for (final Tuple group : this.monotoneQueue.getGroups()) {
                for (final Entry<Tuple, Integer> entry : this.monotoneQueue.getTuplesByGroup(group).entrySet()) {
                    final Tuple update = entry.getKey();
                    final int count = entry.getValue();
                    assert count != 0;
                    final Direction direction = count < 0 ? Direction.DELETE : Direction.INSERT;
                    for (int i = 0; i < Math.abs(count); i++) {
                        this.receiver.updateWithPosetInfo(direction, update, true);
                    }
                }
            }

            this.deliveringMonotone = false;
            swapAndClearMonotone();
        } else {
            throw new IllegalArgumentException("Unsupported message kind " + kind);
        }
    }

    @Override
    public String toString() {
        return "PA_MBOX (" + this.receiver + ") " + this.getActiveMonotoneQueue() + " "
                + this.getActiveAntiMonotoneQueue();
    }

}
