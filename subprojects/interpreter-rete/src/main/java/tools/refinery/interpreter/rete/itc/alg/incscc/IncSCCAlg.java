/*******************************************************************************
 * Copyright (c) 2010-2012, Tamas Szabo, Istvan Rath and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package tools.refinery.interpreter.rete.itc.alg.incscc;

import tools.refinery.interpreter.matchers.algorithms.UnionFind;
import tools.refinery.interpreter.matchers.util.CollectionsFactory;
import tools.refinery.interpreter.matchers.util.Direction;
import tools.refinery.interpreter.matchers.util.IMemoryView;
import tools.refinery.interpreter.rete.itc.alg.counting.CountingAlg;
import tools.refinery.interpreter.rete.itc.alg.misc.DFSPathFinder;
import tools.refinery.interpreter.rete.itc.alg.misc.GraphHelper;
import tools.refinery.interpreter.rete.itc.alg.misc.IGraphPathFinder;
import tools.refinery.interpreter.rete.itc.alg.misc.Tuple;
import tools.refinery.interpreter.rete.itc.alg.misc.bfs.BFS;
import tools.refinery.interpreter.rete.itc.alg.misc.scc.SCC;
import tools.refinery.interpreter.rete.itc.alg.misc.scc.SCCResult;
import tools.refinery.interpreter.rete.itc.alg.util.CollectionHelper;
import tools.refinery.interpreter.rete.itc.graphimpl.Graph;
import tools.refinery.interpreter.rete.itc.igraph.*;

import java.util.*;
import java.util.Map.Entry;

/**
 * Incremental SCC maintenance + counting algorithm.
 *
 * @author Tamas Szabo
 *
 * @param <V>
 *            the type parameter of the nodes in the graph data source
 */
public class IncSCCAlg<V> implements IGraphObserver<V>, ITcDataSource<V> {

    public UnionFind<V> sccs;
    public IBiDirectionalGraphDataSource<V> gds;
    private CountingAlg<V> counting;
    private Graph<V> reducedGraph;
    private IBiDirectionalGraphDataSource<V> reducedGraphIndexer;
    private List<ITcObserver<V>> observers;
    private CountingListener<V> countingListener;

    public IncSCCAlg(IGraphDataSource<V> graphDataSource) {

        if (graphDataSource instanceof IBiDirectionalGraphDataSource<?>) {
            gds = (IBiDirectionalGraphDataSource<V>) graphDataSource;
        } else {
            gds = new IBiDirectionalWrapper<V>(graphDataSource);
        }
        observers = CollectionsFactory.createObserverList();
        sccs = new UnionFind<V>();
        reducedGraph = new Graph<V>();
        reducedGraphIndexer = new IBiDirectionalWrapper<V>(reducedGraph);
        countingListener = new CountingListener<V>(this);
        initalizeInternalDataStructures();
        gds.attachObserver(this);
    }

    private void initalizeInternalDataStructures() {
        SCCResult<V> _sccres = SCC.computeSCC(gds);
        Set<Set<V>> _sccs = _sccres.getSccs();

        for (Set<V> _set : _sccs) {
            sccs.makeSet(_set);
        }

        // Initalization of the reduced graph
        for (V n : sccs.getPartitionHeads()) {
            reducedGraph.insertNode(n);
        }

        for (V source : gds.getAllNodes()) {
            final IMemoryView<V> targetNodes = gds.getTargetNodes(source);
            for (Entry<V, Integer> entry : targetNodes.entriesWithMultiplicities()) {
                for (int i = 0; i < entry.getValue(); i++) {
                    V target = entry.getKey();
                    V sourceRoot = sccs.find(source);
                    V targetRoot = sccs.find(target);

                    if (!sourceRoot.equals(targetRoot)) {
                        reducedGraph.insertEdge(sourceRoot, targetRoot);
                    }
                }
            }
        }

        counting = new CountingAlg<V>(reducedGraph);
    }

    @Override
    public void edgeInserted(V source, V target) {
        V sourceRoot = sccs.find(source);
        V targetRoot = sccs.find(target);

        // Different SCC
        if (!sourceRoot.equals(targetRoot)) {

            // source is reachable from target?
            if (counting.isReachable(targetRoot, sourceRoot)) {

                Set<V> predecessorRoots = counting.getAllReachableSources(sourceRoot);
                Set<V> successorRoots = counting.getAllReachableTargets(targetRoot);

                // 1. intersection of source and target roots, these will be in the merged SCC
                Set<V> isectRoots = CollectionHelper.intersection(predecessorRoots, successorRoots);
                isectRoots.add(sourceRoot);
                isectRoots.add(targetRoot);

                // notifications must be issued before Union-Find modifications
                if (observers.size() > 0) {
                    Set<V> sourceSCCs = createSetNullTolerant(predecessorRoots);
                    sourceSCCs.add(sourceRoot);
                    Set<V> targetSCCs = createSetNullTolerant(successorRoots);
                    targetSCCs.add(targetRoot);

                    // tracing back to actual nodes
                    for (V sourceSCC : sourceSCCs) {
                        targetLoop: for (V targetSCC : targetSCCs) {
                            if (counting.isReachable(sourceSCC, targetSCC)) continue targetLoop;

                            boolean needsNotification =
                                // Case 1. sourceSCC and targetSCC are the same and it is a one sized scc.
                                // Issue notifications only if there is no self-loop present at the moment
                                (sourceSCC.equals(targetSCC) && sccs.getPartition(sourceSCC).size() == 1 && GraphHelper
                                        .getEdgeCount(sccs.getPartition(sourceSCC).iterator().next(), gds) == 0)
                                ||
                                // Case 2. sourceSCC and targetSCC are different sccs.
                                (!sourceSCC.equals(targetSCC));
                            // if self loop is already present omit the notification
                            if (needsNotification) {
                                notifyTcObservers(sccs.getPartition(sourceSCC), sccs.getPartition(targetSCC),
                                        Direction.INSERT);
                            }
                        }
                    }
                }

                // 2. delete edges, nodes
                List<V> sourceSCCs = new ArrayList<V>();
                List<V> targetSCCs = new ArrayList<V>();

                for (V r : isectRoots) {
                    List<V> sourceSCCsOfSCC = getSourceSCCsOfSCC(r);
                    List<V> targetSCCsOfSCC = getTargetSCCsOfSCC(r);

                    for (V sourceSCC : sourceSCCsOfSCC) {
                        if (!sourceSCC.equals(r)) {
                            reducedGraph.deleteEdgeIfExists(sourceSCC, r);
                        }
                    }

                    for (V targetSCC : targetSCCsOfSCC) {
                        if (!isectRoots.contains(targetSCC) && !r.equals(targetSCC)) {
                            reducedGraph.deleteEdgeIfExists(r, targetSCC);
                        }
                    }

                    sourceSCCs.addAll(sourceSCCsOfSCC);
                    targetSCCs.addAll(targetSCCsOfSCC);
                }

                for (V r : isectRoots) {
                    reducedGraph.deleteNode(r);
                }

                // 3. union
                Iterator<V> iterator = isectRoots.iterator();
                V newRoot = iterator.next();
                while (iterator.hasNext()) {
                    newRoot = sccs.union(newRoot, iterator.next());
                }

                // 4. add new node
                reducedGraph.insertNode(newRoot);

                // 5. add edges
                Set<V> containedNodes = sccs.getPartition(newRoot);

                for (V sourceSCC : sourceSCCs) {
                    if (!containedNodes.contains(sourceSCC) && !sourceSCC.equals(newRoot)) {
                        reducedGraph.insertEdge(sourceSCC, newRoot);
                    }
                }
                for (V targetSCC : targetSCCs) {
                    if (!containedNodes.contains(targetSCC) && !targetSCC.equals(newRoot)) {
                        reducedGraph.insertEdge(newRoot, targetSCC);
                    }
                }
            } else {
                if (observers.size() > 0 && GraphHelper.getEdgeCount(source, target, gds) == 1) {
                    counting.attachObserver(countingListener);
                }
                reducedGraph.insertEdge(sourceRoot, targetRoot);
                counting.detachObserver(countingListener);
            }
        } else {
            // Notifications about self-loops
            if (observers.size() > 0 && sccs.getPartition(sourceRoot).size() == 1
                    && GraphHelper.getEdgeCount(source, target, gds) == 1) {
                notifyTcObservers(source, source, Direction.INSERT);
            }
        }
    }

    @Override
    public void edgeDeleted(V source, V target) {
        V sourceRoot = sccs.find(source);
        V targetRoot = sccs.find(target);

        if (!sourceRoot.equals(targetRoot)) {
            if (observers.size() > 0 && GraphHelper.getEdgeCount(source, target, gds) == 0) {
                counting.attachObserver(countingListener);
            }
            reducedGraph.deleteEdgeIfExists(sourceRoot, targetRoot);
            counting.detachObserver(countingListener);
        } else {
            // get the graph for the scc whose root is sourceRoot
            Graph<V> g = GraphHelper.getSubGraph(sccs.getPartition(sourceRoot), gds);

            // if source is not reachable from target anymore
            if (!BFS.isReachable(source, target, g)) {
                // create copies of the current state before destructive manipulation
                Map<V, Integer> reachableSources = CollectionsFactory.createMap();
                for (Entry<V, Integer> entry : reducedGraphIndexer.getSourceNodes(sourceRoot).entriesWithMultiplicities()) {
                    reachableSources.put(entry.getKey(), entry.getValue());
                }
                Map<V, Integer> reachableTargets = CollectionsFactory.createMap();
                for (Entry<V, Integer> entry : reducedGraphIndexer.getTargetNodes(sourceRoot).entriesWithMultiplicities()) {
                    reachableTargets.put(entry.getKey(), entry.getValue());
                }

                SCCResult<V> _newSccs = SCC.computeSCC(g);

                // delete scc node (and with its edges too)
                for (Entry<V, Integer> entry : reachableSources.entrySet()) {
                    V s = entry.getKey();
                    for (int i = 0; i < entry.getValue(); i++) {
                        reducedGraph.deleteEdgeIfExists(s, sourceRoot);
                    }
                }

                for (Entry<V, Integer> entry : reachableTargets.entrySet()) {
                    V t = entry.getKey();
                    for (int i = 0; i < entry.getValue(); i++) {
                        reducedGraph.deleteEdgeIfExists(sourceRoot, t);
                    }
                }

                sccs.deleteSet(sourceRoot);
                reducedGraph.deleteNode(sourceRoot);

                Set<Set<V>> newSCCs = _newSccs.getSccs();
                Set<V> newSCCRoots = CollectionsFactory.createSet();

                // add new nodes and edges to the reduced graph
                for (Set<V> newSCC : newSCCs) {
                    V newRoot = sccs.makeSet(newSCC);
                    reducedGraph.insertNode(newRoot);
                    newSCCRoots.add(newRoot);
                }
                for (V newSCCRoot : newSCCRoots) {
                    List<V> sourceSCCsOfSCC = getSourceSCCsOfSCC(newSCCRoot);
                    List<V> targetSCCsOfSCC = getTargetSCCsOfSCC(newSCCRoot);

                    for (V sourceSCC : sourceSCCsOfSCC) {
                        if (!sourceSCC.equals(newSCCRoot)) {
                            reducedGraph.insertEdge(sccs.find(sourceSCC), newSCCRoot);
                        }
                    }
                    for (V targetSCC : targetSCCsOfSCC) {
                        if (!newSCCRoots.contains(targetSCC) && !targetSCC.equals(newSCCRoot))
                            reducedGraph.insertEdge(newSCCRoot, targetSCC);
                    }
                }

                // Must be after the union-find modifications
                if (observers.size() > 0) {
                    V newSourceRoot = sccs.find(source);
                    V newTargetRoot = sccs.find(target);

                    Set<V> sourceSCCs = createSetNullTolerant(counting.getAllReachableSources(newSourceRoot));
                    sourceSCCs.add(newSourceRoot);

                    Set<V> targetSCCs = createSetNullTolerant(counting.getAllReachableTargets(newTargetRoot));
                    targetSCCs.add(newTargetRoot);

                    for (V sourceSCC : sourceSCCs) {
                        targetLoop: for (V targetSCC : targetSCCs) {
                            if (counting.isReachable(sourceSCC, targetSCC)) continue targetLoop;

                            boolean needsNotification =
                                // Case 1. sourceSCC and targetSCC are the same and it is a one sized scc.
                                // Issue notifications only if there is no self-loop present at the moment
                                (sourceSCC.equals(targetSCC) && sccs.getPartition(sourceSCC).size() == 1 && GraphHelper
                                        .getEdgeCount(sccs.getPartition(sourceSCC).iterator().next(), gds) == 0)
                                ||
                                // Case 2. sourceSCC and targetSCC are different sccs.
                                (!sourceSCC.equals(targetSCC));
                            // if self loop is already present omit the notification
                            if (needsNotification) {
                                notifyTcObservers(sccs.getPartition(sourceSCC), sccs.getPartition(targetSCC),
                                        Direction.DELETE);
                            }
                        }
                    }
                }
            } else {
                // only handle self-loop notifications - sourceRoot equals to targetRoot
                if (observers.size() > 0 && sccs.getPartition(sourceRoot).size() == 1
                        && GraphHelper.getEdgeCount(source, target, gds) == 0) {
                    notifyTcObservers(source, source, Direction.DELETE);
                }
            }
        }
    }

    @Override
    public void nodeInserted(V n) {
        sccs.makeSet(n);
        reducedGraph.insertNode(n);
    }

    @Override
    public void nodeDeleted(V n) {
        IMemoryView<V> sources = gds.getSourceNodes(n);
        IMemoryView<V> targets = gds.getTargetNodes(n);

        for (Entry<V, Integer> entry : sources.entriesWithMultiplicities()) {
            for (int i = 0; i < entry.getValue(); i++) {
                V source = entry.getKey();
                edgeDeleted(source, n);
            }
        }

        for (Entry<V, Integer> entry : targets.entriesWithMultiplicities()) {
            for (int i = 0; i < entry.getValue(); i++) {
                V target = entry.getKey();
                edgeDeleted(n, target);
            }
        }

        sccs.deleteSet(n);
    }

    @Override
    public void attachObserver(ITcObserver<V> to) {
        observers.add(to);
    }

    @Override
    public void detachObserver(ITcObserver<V> to) {
        observers.remove(to);
    }

    @Override
    public Set<V> getAllReachableTargets(V source) {
        V sourceRoot = sccs.find(source);
        Set<V> containedNodes = sccs.getPartition(sourceRoot);
        Set<V> targets = CollectionsFactory.createSet();

        if (containedNodes.size() > 1 || GraphHelper.getEdgeCount(source, gds) == 1) {
            targets.addAll(containedNodes);
        }

        Set<V> rootSet = counting.getAllReachableTargets(sourceRoot);
        if (rootSet != null) {
            for (V _root : rootSet) {
                targets.addAll(sccs.getPartition(_root));
            }
        }

        return targets;
    }

    @Override
    public Set<V> getAllReachableSources(V target) {
        V targetRoot = sccs.find(target);
        Set<V> containedNodes = sccs.getPartition(targetRoot);
        Set<V> sources = CollectionsFactory.createSet();

        if (containedNodes.size() > 1 || GraphHelper.getEdgeCount(target, gds) == 1) {
            sources.addAll(containedNodes);
        }

        Set<V> rootSet = counting.getAllReachableSources(targetRoot);
        if (rootSet != null) {
            for (V _root : rootSet) {
                sources.addAll(sccs.getPartition(_root));
            }
        }
        return sources;
    }

    @Override
    public boolean isReachable(V source, V target) {
        V sourceRoot = sccs.find(source);
        V targetRoot = sccs.find(target);

        if (sourceRoot.equals(targetRoot))
            return true;
        else
            return counting.isReachable(sourceRoot, targetRoot);
    }

    public List<V> getReachabilityPath(V source, V target) {
        if (!isReachable(source, target)) {
            return null;
        } else {
            Set<V> sccsInSubGraph = CollectionHelper.intersection(counting.getAllReachableTargets(source),
                    counting.getAllReachableSources(target));
            sccsInSubGraph.add(sccs.find(source));
            sccsInSubGraph.add(sccs.find(target));
            Set<V> nodesInSubGraph = CollectionsFactory.createSet();

            for (V sccRoot : sccsInSubGraph) {
                nodesInSubGraph.addAll(sccs.getPartition(sccRoot));
            }

            return GraphHelper.constructPath(source, target, nodesInSubGraph, gds);
        }
    }

    /**
     * Return the SCCs from which the SCC represented by the root node is reachable. Note that an SCC can be present
     * multiple times in the returned list (multiple edges between the two SCCs).
     *
     * @param root
     * @return the list of reachable target SCCs
     */
    private List<V> getSourceSCCsOfSCC(V root) {
        List<V> sourceSCCs = new ArrayList<V>();

        for (V containedNode : this.sccs.getPartition(root)) {
            IMemoryView<V> sourceNodes = this.gds.getSourceNodes(containedNode);
            for (V source : sourceNodes.distinctValues()) {
                sourceSCCs.add(this.sccs.find(source));
            }
        }

        return sourceSCCs;
    }

    /**
     * Returns true if the SCC represented by the given root node has incoming edges in the reduced graph,
     * false otherwise (if this SCC is a source in the reduced graph).
     *
     * @param root the root node of an SCC
     * @return true if it has incoming edges, false otherwise
     * @since 1.6
     */
    public boolean hasIncomingEdges(final V root) {
        for (final V containedNode : this.sccs.getPartition(root)) {
            final IMemoryView<V> sourceNodes = this.gds.getSourceNodes(containedNode);
            for (final V source : sourceNodes.distinctValues()) {
                final V otherRoot = this.sccs.find(source);
                if (!Objects.equals(root, otherRoot)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Return the SCCs which are reachable from the SCC represented by the root node. Note that an SCC can be present
     * multiple times in the returned list (multiple edges between the two SCCs).
     *
     * @param root
     * @return the list of reachable target SCCs
     */
    private List<V> getTargetSCCsOfSCC(V root) {
        List<V> targetSCCs = new ArrayList<V>();

        for (V containedNode : this.sccs.getPartition(root)) {
            IMemoryView<V> targetNodes = this.gds.getTargetNodes(containedNode);
            for (V target : targetNodes.distinctValues()) {
                targetSCCs.add(this.sccs.find(target));
            }
        }

        return targetSCCs;
    }

    /**
     * Returns true if the SCC represented by the given root node has outgoing edges in the reduced graph,
     * false otherwise (if this SCC is a sink in the reduced graph).
     *
     * @param root the root node of an SCC
     * @return true if it has outgoing edges, false otherwise
     * @since 1.6
     */
    public boolean hasOutgoingEdges(V root) {
        for (final V containedNode : this.sccs.getPartition(root)) {
            final IMemoryView<V> targetNodes = this.gds.getTargetNodes(containedNode);
            for (final V target : targetNodes.distinctValues()) {
                final V otherRoot = this.sccs.find(target);
                if (!Objects.equals(root, otherRoot)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public void dispose() {
        gds.detachObserver(this);
        counting.dispose();
    }

    /**
     * Call this method to notify the observers of the transitive closure relation. The tuples used in the notification
     * will be the Descartes product of the two sets given.
     *
     * @param sources
     *            the source nodes
     * @param targets
     *            the target nodes
     * @param direction
     */
    protected void notifyTcObservers(Set<V> sources, Set<V> targets, Direction direction) {
        for (V s : sources) {
            for (V t : targets) {
                notifyTcObservers(s, t, direction);
            }
        }
    }

    private void notifyTcObservers(V source, V target, Direction direction) {
        for (ITcObserver<V> observer : observers) {
            if (direction == Direction.INSERT) {
                observer.tupleInserted(source, target);
            }
            if (direction == Direction.DELETE) {
                observer.tupleDeleted(source, target);
            }
        }
    }

    /**
     * Returns the node that is selected as the representative of the SCC containing the argument.
     * @since 1.6
     */
    public V getRepresentative(V node) {
        return sccs.find(node);
    }

    public Set<Tuple<V>> getTcRelation() {
        Set<Tuple<V>> resultSet = new HashSet<Tuple<V>>();

        for (V sourceRoot : sccs.getPartitionHeads()) {
            Set<V> sources = sccs.getPartition(sourceRoot);
            if (sources.size() > 1 || GraphHelper.getEdgeCount(sources.iterator().next(), gds) == 1) {
                for (V source : sources) {
                    for (V target : sources) {
                        resultSet.add(new Tuple<V>(source, target));
                    }
                }
            }

            Set<V> reachableTargets = counting.getAllReachableTargets(sourceRoot);
            if (reachableTargets != null) {
                for (V targetRoot : reachableTargets) {
                    for (V source : sources) {
                        for (V target : sccs.getPartition(targetRoot)) {
                            resultSet.add(new Tuple<V>(source, target));
                        }
                    }
                }
            }
        }

        return resultSet;
    }

    public boolean isIsolated(V node) {
        IMemoryView<V> targets = gds.getTargetNodes(node);
        IMemoryView<V> sources = gds.getSourceNodes(node);
        return targets.isEmpty() && sources.isEmpty();
    }

    @Override
    public IGraphPathFinder<V> getPathFinder() {
        return new DFSPathFinder<V>(gds, this);
    }

    /**
     * The graph of SCCs; each SCC is represented by its representative node (see {@link #getRepresentative(Object)})
     * @since 1.6
     */
    public Graph<V> getReducedGraph() {
        return reducedGraph;
    }

    private static <V> Set<V> createSetNullTolerant(Set<V> initial) {
        if (initial != null)
            return CollectionsFactory.createSet(initial);
        else
            return CollectionsFactory.createSet();
    }


}
