/*******************************************************************************
 * Copyright (c) 2004-2012 Gabor Bergmann and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package tools.refinery.interpreter.rete.index;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import tools.refinery.interpreter.rete.network.Node;
import tools.refinery.interpreter.rete.network.ReteContainer;
import tools.refinery.interpreter.rete.network.Supplier;
import tools.refinery.interpreter.rete.network.communication.CommunicationTracker;
import tools.refinery.interpreter.rete.network.communication.Timestamp;
import tools.refinery.interpreter.matchers.tuple.Tuple;
import tools.refinery.interpreter.matchers.tuple.TupleMask;
import tools.refinery.interpreter.matchers.util.Direction;

/**
 * A specialized projection indexer that can be memory-less (relying on an external source of information).
 *
 * <p>
 * All specialized projection indexers of a single node will share the same listener list, so that notification order is
 * maintained (see Bug 518434).
 *
 * @author Gabor Bergmann
 * @noimplement Rely on the provided implementations
 * @noreference Use only via standard Node and Indexer interfaces
 * @noinstantiate This class is not intended to be instantiated by clients.
 */
public abstract class SpecializedProjectionIndexer extends StandardIndexer implements ProjectionIndexer {

    protected Node activeNode;
    protected List<ListenerSubscription> subscriptions;

    /**
     * @since 1.7
     */
    public SpecializedProjectionIndexer(final ReteContainer reteContainer, final TupleMask mask, final Supplier parent,
                                        final Node activeNode, final List<ListenerSubscription> subscriptions) {
        super(reteContainer, mask);
        this.parent = parent;
        this.activeNode = activeNode;
        this.subscriptions = subscriptions;
    }

    public List<ListenerSubscription> getSubscriptions() {
        return subscriptions;
    }

    @Override
    public Node getActiveNode() {
        return activeNode;
    }

    @Override
    protected void propagate(final Direction direction, final Tuple updateElement, final Tuple signature,
            final boolean change, final Timestamp timestamp) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void attachListener(final IndexerListener listener) {
        super.attachListener(listener);
        final CommunicationTracker tracker = this.getCommunicationTracker();
        final IndexerListener proxy = tracker.proxifyIndexerListener(this, listener);
        final ListenerSubscription subscription = new ListenerSubscription(this, proxy);
        tracker.registerDependency(this, proxy.getOwner());
        // See Bug 518434
        // Must add to the first position, so that the later listeners are notified earlier.
        // Thus if the beta node added as listener is also an indirect descendant of the same indexer on its opposite
        // slot,
        // then the beta node is connected later than its ancestor's listener, therefore it will be notified earlier,
        // eliminating duplicate insertions and lost deletions that would result from fall-through update propagation
        subscriptions.add(0, subscription);
    }

    @Override
    public void detachListener(final IndexerListener listener) {
        final CommunicationTracker tracker = this.getCommunicationTracker();
        // obtain the proxy before the super call would unregister the dependency
        final IndexerListener proxy = tracker.proxifyIndexerListener(this, listener);
        super.detachListener(listener);
        final ListenerSubscription subscription = new ListenerSubscription(this, proxy);
        final boolean wasContained = subscriptions.remove(subscription);
        assert wasContained;
        tracker.unregisterDependency(this, proxy.getOwner());
    }

    @Override
    public void networkStructureChanged() {
        super.networkStructureChanged();
        final List<ListenerSubscription> oldSubscriptions = new ArrayList<ListenerSubscription>();
        oldSubscriptions.addAll(subscriptions);
        subscriptions.clear();
        for (final ListenerSubscription oldSubscription : oldSubscriptions) {
            // there is no need to unregister and re-register the dependency between indexer and listener
            // because the owner of the listener is the same (even if it is proxified)
            final CommunicationTracker tracker = this.getCommunicationTracker();
            // the subscriptions are shared, so we MUST reuse the indexer of the subscription instead of simply 'this'
            final IndexerListener proxy = tracker.proxifyIndexerListener(oldSubscription.indexer, oldSubscription.listener);
            final ListenerSubscription newSubscription = new ListenerSubscription(oldSubscription.indexer, proxy);
            subscriptions.add(newSubscription);
        }
    }

    /**
     * @since 2.4
     */
    public abstract void propagateToListener(IndexerListener listener, Direction direction, Tuple updateElement,
            Timestamp timestamp);

    /**
     * Infrastructure to share subscriptions between specialized indexers of the same parent node.
     *
     * @author Gabor Bergmann
     * @since 1.7
     */
    public static class ListenerSubscription {
        protected SpecializedProjectionIndexer indexer;
        protected IndexerListener listener;

        public ListenerSubscription(SpecializedProjectionIndexer indexer, IndexerListener listener) {
            super();
            this.indexer = indexer;
            this.listener = listener;
        }

        /**
         * @since 2.4
         */
        public SpecializedProjectionIndexer getIndexer() {
            return indexer;
        }

        /**
         * @since 2.4
         */
        public IndexerListener getListener() {
            return listener;
        }

        /**
         * Call this from parent node.
         * @since 2.4
         */
        public void propagate(Direction direction, Tuple updateElement, Timestamp timestamp) {
            indexer.propagateToListener(listener, direction, updateElement, timestamp);
        }

        @Override
        public int hashCode() {
            return Objects.hash(indexer, listener);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            ListenerSubscription other = (ListenerSubscription) obj;
            return Objects.equals(listener, other.listener) && Objects.equals(indexer, other.indexer);
        }

    }

}
