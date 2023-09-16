/*******************************************************************************
 * Copyright (c) 2010-2012, Tamas Szabo, Istvan Rath and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package tools.refinery.interpreter.rete.itc.graphimpl;

import tools.refinery.interpreter.rete.itc.igraph.IGraphDataSource;
import tools.refinery.interpreter.rete.itc.igraph.IGraphObserver;
import tools.refinery.interpreter.rete.itc.igraph.IBiDirectionalGraphDataSource;
import tools.refinery.interpreter.matchers.util.CollectionsFactory;
import tools.refinery.interpreter.matchers.util.CollectionsFactory.MemoryType;
import tools.refinery.interpreter.matchers.util.IMemoryView;
import tools.refinery.interpreter.matchers.util.IMultiLookup;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class Graph<V> implements IGraphDataSource<V>, IBiDirectionalGraphDataSource<V> {

    // source -> target -> count
    private IMultiLookup<V, V> outgoingEdges;
    // target -> source -> count
    private IMultiLookup<V, V> incomingEdges;

    private Set<V> nodes;

    private List<IGraphObserver<V>> observers;

    public Graph() {
        outgoingEdges = CollectionsFactory.createMultiLookup(Object.class, MemoryType.MULTISETS, Object.class);
        incomingEdges = CollectionsFactory.createMultiLookup(Object.class, MemoryType.MULTISETS, Object.class);
        nodes = CollectionsFactory.createSet();
        observers = CollectionsFactory.createObserverList();
    }

    public void insertEdge(V source, V target) {
        outgoingEdges.addPair(source, target);
        incomingEdges.addPair(target, source);

        for (IGraphObserver<V> go : observers) {
            go.edgeInserted(source, target);
        }
    }

    /**
     * No-op if trying to delete edge that does not exist
     *
     * @since 2.0
     * @see #deleteEdgeIfExists(Object, Object)
     */
    public void deleteEdgeIfExists(V source, V target) {
        boolean containedEdge = outgoingEdges.lookupOrEmpty(source).containsNonZero(target);
        if (containedEdge) {
            deleteEdgeThatExists(source, target);
        }
    }

    /**
     * @throws IllegalStateException
     *             if trying to delete edge that does not exist
     * @since 2.0
     * @see #deleteEdgeIfExists(Object, Object)
     */
    public void deleteEdgeThatExists(V source, V target) {
        outgoingEdges.removePair(source, target);
        incomingEdges.removePair(target, source);
        for (IGraphObserver<V> go : observers) {
            go.edgeDeleted(source, target);
        }
    }

    /**
     * @deprecated use explicitly {@link #deleteEdgeThatExists(Object, Object)} or
     *             {@link #deleteEdgeIfExists(Object, Object)} instead. To preserve backwards compatibility, this method
     *             delegates to the latter.
     *
     */
    @Deprecated
    public void deleteEdge(V source, V target) {
        deleteEdgeIfExists(source, target);
    }

    /**
     * Insert the given node into the graph.
     */
    public void insertNode(V node) {
        if (nodes.add(node)) {
            for (IGraphObserver<V> go : observers) {
                go.nodeInserted(node);
            }
        }
    }

    /**
     * Deletes the given node AND all of the edges going in and out from the node.
     */
    public void deleteNode(V node) {
        if (nodes.remove(node)) {
            IMemoryView<V> incomingView = incomingEdges.lookup(node);
            if (incomingView != null) {
                Map<V, Integer> incoming = CollectionsFactory.createMap(incomingView.asMap());

                for (Entry<V, Integer> entry : incoming.entrySet()) {
                    for (int i = 0; i < entry.getValue(); i++) {
                        deleteEdgeThatExists(entry.getKey(), node);
                    }
                }
            }

            IMemoryView<V> outgoingView = outgoingEdges.lookup(node);
            if (outgoingView != null) {
                Map<V, Integer> outgoing = CollectionsFactory.createMap(outgoingView.asMap());

                for (Entry<V, Integer> entry : outgoing.entrySet()) {
                    for (int i = 0; i < entry.getValue(); i++) {
                        deleteEdgeThatExists(node, entry.getKey());
                    }
                }
            }

            for (IGraphObserver<V> go : observers) {
                go.nodeDeleted(node);
            }
        }
    }

    @Override
    public void attachObserver(IGraphObserver<V> go) {
        observers.add(go);
    }

    @Override
    public void attachAsFirstObserver(IGraphObserver<V> observer) {
        observers.add(0, observer);
    }

    @Override
    public void detachObserver(IGraphObserver<V> go) {
        observers.remove(go);
    }

    @Override
    public Set<V> getAllNodes() {
        return nodes;
    }

    @Override
    public IMemoryView<V> getTargetNodes(V source) {
        return outgoingEdges.lookupOrEmpty(source);
    }

    @Override
    public IMemoryView<V> getSourceNodes(V target) {
        return incomingEdges.lookupOrEmpty(target);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("nodes = ");
        for (V n : getAllNodes()) {
            sb.append(n.toString());
            sb.append(" ");
        }
        sb.append(" edges = ");
        for (V source : outgoingEdges.distinctKeys()) {
            IMemoryView<V> targets = outgoingEdges.lookup(source);
            for (V target : targets.distinctValues()) {
                int count = targets.getCount(target);
                for (int i = 0; i < count; i++) {
                    sb.append("(" + source + "," + target + ") ");
                }
            }
        }
        return sb.toString();
    }

}
