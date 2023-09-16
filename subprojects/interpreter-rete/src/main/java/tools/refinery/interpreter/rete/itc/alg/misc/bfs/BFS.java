/*******************************************************************************
 * Copyright (c) 2010-2012, Tamas Szabo, Istvan Rath and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package tools.refinery.interpreter.rete.itc.alg.misc.bfs;

import tools.refinery.interpreter.rete.itc.igraph.IBiDirectionalGraphDataSource;
import tools.refinery.interpreter.rete.itc.igraph.IGraphDataSource;

import java.util.*;

public class BFS<V> {

    private BFS() {/*Utility class constructor*/}

    /**
     * Performs a breadth first search on the given graph to determine whether source is reachable from target.
     *
     * @param <V>
     *            the type parameter of the nodes in the graph
     * @param source
     *            the source node
     * @param target
     *            the target node
     * @param graph
     *            the graph data source
     * @return true if source is reachable from target, false otherwise
     */
    public static <V> boolean isReachable(V source, V target, IGraphDataSource<V> graph) {
        Deque<V> nodeQueue = new ArrayDeque<V>();
        Set<V> visited = new HashSet<V>();

        nodeQueue.add(source);
        visited.add(source);

        boolean ret = _isReachable(target, graph, nodeQueue, visited);
        return ret;
    }

    private static <V> boolean _isReachable(V target, IGraphDataSource<V> graph, Deque<V> nodeQueue, Set<V> visited) {

        while (!nodeQueue.isEmpty()) {
            V node = nodeQueue.removeFirst();
            for (V t : graph.getTargetNodes(node).distinctValues()){
                if (t.equals(target)) {
                    return true;
                }
                if (!visited.contains(t)) {
                    visited.add(t);
                    nodeQueue.addLast(t);
                }
            }
        }
        return false;
    }

    public static <V> Set<V> reachableSources(IBiDirectionalGraphDataSource<V> graph, V target) {
        Set<V> retSet = new HashSet<V>();
        retSet.add(target);
        Deque<V> nodeQueue = new ArrayDeque<V>();
        nodeQueue.add(target);

        _reachableSources(graph, nodeQueue, retSet);

        return retSet;
    }

    private static <V> void _reachableSources(IBiDirectionalGraphDataSource<V> graph, Deque<V> nodeQueue,
            Set<V> retSet) {
        while (!nodeQueue.isEmpty()) {
            V node = nodeQueue.removeFirst();
            for (V _node : graph.getSourceNodes(node).distinctValues()) {
                if (!retSet.contains(_node)) {
                    retSet.add(_node);
                    nodeQueue.addLast(_node);
                }
            }
        }
    }

    public static <V> Set<V> reachableTargets(IGraphDataSource<V> graph, V source) {
        Set<V> retSet = new HashSet<V>();
        retSet.add(source);
        Deque<V> nodeQueue = new ArrayDeque<V>();
        nodeQueue.add(source);

        _reachableTargets(graph, nodeQueue, retSet);

        return retSet;
    }

    private static <V> void _reachableTargets(IGraphDataSource<V> graph, Deque<V> nodeQueue, Set<V> retSet) {
        while (!nodeQueue.isEmpty()) {
            V node = nodeQueue.removeFirst();

            for (V _node : graph.getTargetNodes(node).distinctValues()) {

                if (!retSet.contains(_node)) {
                    retSet.add(_node);
                    nodeQueue.addLast(_node);
                }
            }
        }
    }

    /**
     * Performs a breadth first search on the given graph and collects all the nodes along the path from source to
     * target if such path exists.
     *
     * @param <V>
     *            the type parameter of the nodes in the graph
     * @param source
     *            the source node
     * @param target
     *            the target node
     * @param graph
     *            the graph data source
     * @return the set of nodes along the path
     */
    public static <V> Set<V> collectNodesAlongPath(V source, V target, IGraphDataSource<V> graph) {
        Set<V> path = new HashSet<V>();
        _collectNodesAlongPath(source, target, graph, path);
        return path;
    }

    private static <V> boolean _collectNodesAlongPath(V node, V target, IGraphDataSource<V> graph, Set<V> path) {

        boolean res = false;

        // end recursion
        if (node.equals(target)) {
            path.add(node);
            return true;
        } else {
            for (V _nodeT : graph.getTargetNodes(node).distinctValues()) {
                res = (_collectNodesAlongPath(_nodeT, target, graph, path)) || res;
            }
            if (res)
                path.add(node);
            return res;
        }
    }
}
