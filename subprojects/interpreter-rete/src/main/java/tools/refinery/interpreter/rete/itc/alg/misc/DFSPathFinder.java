/*******************************************************************************
 * Copyright (c) 2010-2016, Abel Hegedus and IncQueryLabs Ltd.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.interpreter.rete.itc.alg.misc;

import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import tools.refinery.interpreter.rete.itc.igraph.IGraphDataSource;
import tools.refinery.interpreter.rete.itc.igraph.ITcDataSource;
import tools.refinery.interpreter.matchers.util.IMemoryView;

/**
 * A depth-first search implementation of the {@link IGraphPathFinder}.
 *
 * TODO use ITC to filter nodes that must be traversed, instead of checks
 *
 * @author Abel Hegedus
 *
 * @param <V>
 *            the node type of the graph
 */
public class DFSPathFinder<V> implements IGraphPathFinder<V> {

    private IGraphDataSource<V> graph;
    private ITcDataSource<V> itc;

    public DFSPathFinder(IGraphDataSource<V> graph, ITcDataSource<V> itc) {
        this.graph = graph;
        this.itc = itc;
    }

    @Override
    public Iterable<Deque<V>> getAllPaths(V sourceNode, V targetNode) {
        Set<V> endNodes = new HashSet<V>();
        endNodes.add(targetNode);
        return getAllPathsToTargets(sourceNode, endNodes);
    }

    @Override
    public Iterable<Deque<V>> getAllPathsToTargets(V sourceNode, Set<V> targetNodes) {
        List<Deque<V>> paths = new ArrayList<Deque<V>>();
        Deque<V> visited = new LinkedList<V>();
        Set<V> reachableTargets = new HashSet<V>();
        for (V targetNode : targetNodes) {
            if (itc.isReachable(sourceNode, targetNode)) {
                reachableTargets.add(targetNode);
            }
        }
        if (!reachableTargets.isEmpty()) {
            return paths;
        }
        visited.add(sourceNode);
        return getPaths(paths, visited, reachableTargets);
    }

    protected Iterable<Deque<V>> getPaths(List<Deque<V>> paths, Deque<V> visited, Set<V> targetNodes) {
        IMemoryView<V> nodes = graph.getTargetNodes(visited.getLast());
        // examine adjacent nodes
        for (V node : nodes.distinctValues()) {
            if (visited.contains(node)) {
                continue;
            }
            if (targetNodes.contains(node)) {
                visited.add(node);
                // clone visited LinkedList
                Deque<V> visitedClone = new LinkedList<V>(visited);
                paths.add(visitedClone);
                visited.removeLast();
                break;
            }
        }

        // in breadth-first, recursion needs to come after visiting connected nodes
        for (V node : nodes.distinctValues()) {
            if (visited.contains(node) || targetNodes.contains(node)) {
                continue;
            }
            boolean canReachTarget = false;
            for (V target : targetNodes) {
                if (itc.isReachable(node, target)) {
                    canReachTarget = true;
                    break;
                }
            }
            if (canReachTarget) {
                visited.addLast(node);
                getPaths(paths, visited, targetNodes);
                visited.removeLast();
            }
        }

        return paths;
    }

    public String printPaths(List<Deque<V>> paths) {
        StringBuilder sb = new StringBuilder();
        for (Deque<V> visited : paths) {
            sb.append("Path: ");
            for (V node : visited) {
                sb.append(node);
                sb.append(" --> ");
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    @Override
    public Deque<V> getPath(V sourceNode, V targetNode) {
        // TODO optimize
        Iterable<Deque<V>> allPaths = getAllPaths(sourceNode, targetNode);
        Iterator<Deque<V>> pathIterator = allPaths.iterator();
        return pathIterator.hasNext() ? pathIterator.next() : new LinkedList<V>();
    }

    @Override
    public Iterable<Deque<V>> getShortestPaths(V sourceNode, V targetNode) {
        // TODO optimize
        Iterable<Deque<V>> allPaths = getAllPaths(sourceNode, targetNode);
        List<Deque<V>> shortestPaths = new ArrayList<Deque<V>>();
        int shortestPathLength = -1;
        for (Deque<V> path : allPaths) {
            int pathLength = path.size();
            if (shortestPathLength == -1 || pathLength < shortestPathLength) {
                shortestPaths.clear();
                shortestPathLength = pathLength;
            }
            if (pathLength == shortestPathLength) {
                shortestPaths.add(path);
            }
        }
        return shortestPaths;
    }

}
