/*******************************************************************************
 * Copyright (c) 2004-2008 Gabor Bergmann and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package tools.refinery.interpreter.rete.network;

import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;

import tools.refinery.interpreter.rete.network.communication.Timestamp;
import tools.refinery.interpreter.rete.network.mailbox.Mailbox;
import tools.refinery.interpreter.matchers.tuple.Tuple;
import tools.refinery.interpreter.matchers.util.Direction;

/**
 * ALL METHODS: FOR INTERNAL USE ONLY; ONLY INVOKE FROM {@link ReteContainer}
 *
 * @author Gabor Bergmann
 * @noimplement This interface is not intended to be implemented by external clients.
 */
public interface Receiver extends Node {

    /**
     * Updates the receiver with a newly found or lost partial matching.
     *
     * @since 2.4
     */
    public void update(final Direction direction, final Tuple updateElement, final Timestamp timestamp);

    /**
     * Updates the receiver in batch style with a collection of updates. The input collection consists of pairs in the
     * form (t, c) where t is an update tuple and c is the count. The count can also be negative, and it specifies how
     * many times the tuple t gets deleted or inserted. The default implementation of this method simply calls
     * {@link #update(Direction, Tuple, Timestamp)} individually for all updates.
     *
     * @since 2.8
     */
    public default void batchUpdate(final Collection<Map.Entry<Tuple, Integer>> updates, final Timestamp timestamp) {
        for (final Entry<Tuple, Integer> entry : updates) {
            int count = entry.getValue();

            Direction direction;
            if (count < 0) {
                direction = Direction.DELETE;
                count = -count;
            } else {
                direction = Direction.INSERT;
            }

            for (int i = 0; i < count; i++) {
                update(direction, entry.getKey(), timestamp);
            }
        }
    }

    /**
     * Returns the {@link Mailbox} of this receiver.
     *
     * @return the mailbox
     * @since 2.0
     */
    public Mailbox getMailbox();

    /**
     * appends a parent that will continuously send insert and revoke updates to this supplier
     */
    void appendParent(final Supplier supplier);

    /**
     * removes a parent
     */
    void removeParent(final Supplier supplier);

    /**
     * access active parent
     */
    Collection<Supplier> getParents();

}
