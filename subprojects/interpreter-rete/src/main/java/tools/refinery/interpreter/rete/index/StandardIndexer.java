/*******************************************************************************
 * Copyright (c) 2004-2008 Gabor Bergmann and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package tools.refinery.interpreter.rete.index;

import java.util.Collection;
import java.util.List;

import tools.refinery.interpreter.rete.network.BaseNode;
import tools.refinery.interpreter.rete.network.NetworkStructureChangeSensitiveNode;
import tools.refinery.interpreter.rete.network.ReteContainer;
import tools.refinery.interpreter.rete.network.Supplier;
import tools.refinery.interpreter.rete.network.communication.Timestamp;
import tools.refinery.interpreter.rete.traceability.TraceInfo;
import tools.refinery.interpreter.matchers.tuple.Tuple;
import tools.refinery.interpreter.matchers.tuple.TupleMask;
import tools.refinery.interpreter.matchers.util.CollectionsFactory;
import tools.refinery.interpreter.matchers.util.Direction;

/**
 * An abstract standard implementation of the Indexer interface, providing common bookkeeping functionality.
 *
 * @author Gabor Bergmann
 *
 */
public abstract class StandardIndexer extends BaseNode implements Indexer, NetworkStructureChangeSensitiveNode {

    protected Supplier parent;
    private final List<IndexerListener> originalListeners;
    private final List<IndexerListener> proxyListeners;
    protected TupleMask mask;

    public StandardIndexer(ReteContainer reteContainer, TupleMask mask) {
        super(reteContainer);
        this.parent = null;
        this.mask = mask;
        this.originalListeners = CollectionsFactory.createObserverList();
        this.proxyListeners = CollectionsFactory.createObserverList();
    }

    /**
     * @since 2.4
     */
    protected void propagate(Direction direction, Tuple updateElement, Tuple signature, boolean change, Timestamp timestamp) {
        for (IndexerListener listener : proxyListeners) {
            listener.notifyIndexerUpdate(direction, updateElement, signature, change, timestamp);
        }
    }

    @Override
    public TupleMask getMask() {
        return mask;
    }

    @Override
    public Supplier getParent() {
        return parent;
    }

    @Override
    public void attachListener(IndexerListener listener) {
        this.getCommunicationTracker().registerDependency(this, listener.getOwner());
        // obtain the proxy after registering the dependency because then the proxy reflects the new SCC structure
        final IndexerListener proxy = this.getCommunicationTracker().proxifyIndexerListener(this, listener);
        // See Bug 518434
        // Must add to the first position, so that the later listeners are notified earlier.
        // Thus if the beta node added as listener is also an indirect descendant of the same indexer on its opposite slot,
        // then the beta node is connected later than its ancestor's listener, therefore it will be notified earlier,
        // eliminating duplicate insertions and lost deletions that would result from fall-through update propagation
        this.originalListeners.add(0, listener);
        this.proxyListeners.add(0, proxy);
    }

    @Override
    public void detachListener(IndexerListener listener) {
        this.originalListeners.remove(listener);
        IndexerListener listenerToRemove = null;
        for (final IndexerListener proxyListener : this.proxyListeners) {
            if (proxyListener.getOwner() == listener.getOwner()) {
                listenerToRemove = proxyListener;
                break;
            }
        }
        assert listenerToRemove != null;
        this.proxyListeners.remove(listenerToRemove);
        this.getCommunicationTracker().unregisterDependency(this, listener.getOwner());
    }

    @Override
    public void networkStructureChanged() {
        this.proxyListeners.clear();
        for (final IndexerListener original : this.originalListeners) {
            this.proxyListeners.add(this.getCommunicationTracker().proxifyIndexerListener(this, original));
        }
    }

    @Override
    public Collection<IndexerListener> getListeners() {
        return proxyListeners;
    }

    @Override
    public ReteContainer getContainer() {
        return reteContainer;
    }

    @Override
    protected String toStringCore() {
        return super.toStringCore() + "(" + parent + "/" + mask + ")";
    }

    @Override
    public void assignTraceInfo(TraceInfo traceInfo) {
        super.assignTraceInfo(traceInfo);
        if (traceInfo.propagateFromIndexerToSupplierParent())
            if (parent != null)
                parent.acceptPropagatedTraceInfo(traceInfo);
    }


}
