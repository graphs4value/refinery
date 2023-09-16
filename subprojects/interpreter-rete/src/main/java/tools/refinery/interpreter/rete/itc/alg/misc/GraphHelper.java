/*******************************************************************************
 * Copyright (c) 2010-2013, Tamas Szabo, Istvan Rath and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.interpreter.rete.itc.alg.misc;

import tools.refinery.interpreter.rete.itc.graphimpl.Graph;
import tools.refinery.interpreter.rete.itc.igraph.IBiDirectionalGraphDataSource;
import tools.refinery.interpreter.rete.itc.igraph.IGraphDataSource;
import tools.refinery.interpreter.matchers.util.IMemoryView;

import java.util.*;
import java.util.Map.Entry;

/**
 * Utility class for graph related operations.
 *
 * @author Tamas Szabo
 */
public class GraphHelper {

    private GraphHelper() {/*Utility class constructor*/}

    /**
     * Returns the subgraph from the given {@link IBiDirectionalGraphDataSource} which contains the given set of nodes.
     *
     * @param nodesInSubGraph
     *            the nodes that are present in the subgraph
     * @param graphDataSource
     *            the graph data source for the original graph
     * @return the subgraph associated to the given nodes
     */
    public static <V> Graph<V> getSubGraph(Collection<V> nodesInSubGraph,
                                           IBiDirectionalGraphDataSource<V> graphDataSource) {
        Graph<V> g = new Graph<V>();
        if (nodesInSubGraph != null) {
            for (V node : nodesInSubGraph) {
                g.insertNode(node);
            }

            for (V node : nodesInSubGraph) {
                IMemoryView<V> sources = graphDataSource.getSourceNodes(node);
                for (Entry<V, Integer> entry : sources.entriesWithMultiplicities()) {
                    for (int i = 0; i < entry.getValue(); i++) {
                        V s = entry.getKey();
                        if (nodesInSubGraph.contains(s)) {
                            g.insertEdge(s, node);
                        }
                    }
                }
            }
        }

        return g;
    }

    /**
     * Constructs a path between source and target in the given graph. Both the {@link IGraphDataSource} and the set of
     * nodes are used, this way it is possible to construct a path in a given subgraph.
     *
     * The returned {@link List} contains the nodes along the path (this means that there is an edge in the graph
     * between two consecutive nodes). A self loop (one edge) is indicated with the source node being present two times
     * in the returned {@link List}.
     *
     * @param source
     *            the source node
     * @param target
     *            the target node
     * @param nodesInGraph
     *            the nodes that are present in the subgraph
     * @param graphDataSource
     *            the graph data source
     * @return the path between the two nodes
     */
    public static <V> List<V> constructPath(V source, V target, Set<V> nodesInGraph,
            IGraphDataSource<V> graphDataSource) {
        Set<V> visitedNodes = new HashSet<V>();
        List<V> path = new ArrayList<V>();

        visitedNodes.add(source);
        path.add(source);
        V act = source;

        // if source and target are the same node
        if (source.equals(target) && graphDataSource.getTargetNodes(source).containsNonZero(target)) {
            // the node will be present in the path two times
            path.add(source);
            return path;
        } else {
            while (act != null) {
                V nextNode = getNextNodeToVisit(act, graphDataSource, nodesInGraph, visitedNodes);
                if (nextNode == null && path.size() > 1) {
                    // needs to backtrack along path
                    // remove the last element in the path because we can't go
                    // anywhere from there
                    path.remove(path.size() - 1);
                    while (nextNode == null && path.size() > 0) {
                        V lastPathElement = path.get(path.size() - 1);
                        nextNode = getNextNodeToVisit(lastPathElement, graphDataSource, nodesInGraph, visitedNodes);
                        if (nextNode == null) {
                            path.remove(path.size() - 1);
                        }
                    }
                }

                if (nextNode != null) {
                    visitedNodes.add(nextNode);
                    path.add(nextNode);
                    if (nextNode.equals(target)) {
                        return path;
                    }
                }
                act = nextNode;
            }
            return null;
        }
    }

    private static <V> V getNextNodeToVisit(V act, IGraphDataSource<V> graphDataSource, Set<V> nodesInSubGraph,
            Set<V> visitedNodes) {
        IMemoryView<V> targetNodes = graphDataSource.getTargetNodes(act);
        for (Entry<V, Integer> entry : targetNodes.entriesWithMultiplicities()) {
            for (int i = 0; i < entry.getValue(); i++) {
                V node = entry.getKey();
                if (nodesInSubGraph.contains(node) && !visitedNodes.contains(node)) {
                    return node;
                }
            }
        }
        return null;
    }

    /**
     * Returns the number of self-loop edges for the given node.
     *
     * @param node
     *            the node
     * @param graphDataSource
     *            the graph data source
     * @return the number of self-loop edges
     */
    public static <V> int getEdgeCount(V node, IGraphDataSource<V> graphDataSource) {
        return getEdgeCount(node, node, graphDataSource);
    }

    /**
     * Returns the number of edges between the given source and target nodes.
     *
     * @param source
     *            the source node
     * @param target
     *            the target node
     * @param graphDataSource
     *            the graph data source
     * @return the number of parallel edges between the two nodes
     */
    public static <V> int getEdgeCount(V source, V target, IGraphDataSource<V> graphDataSource) {
        Integer count = graphDataSource.getTargetNodes(source).getCount(target);
        if (count == null) {
            return 0;
        } else {
            return count;
        }
    }
}
