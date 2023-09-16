/*******************************************************************************
 * Copyright (c) 2010-2013, Abel Hegedus, Istvan Rath and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.interpreter.rete.itc.alg.misc;

import java.util.Deque;
import java.util.Set;

import tools.refinery.interpreter.rete.itc.igraph.ITcDataSource;

/**
 * The path finder provides methods for retrieving paths in a graph between a source node and one or more target nodes.
 * Use {@link ITcDataSource#getPathFinder()} for instantiating.
 *
 * @author Abel Hegedus
 *
 * @param <V> the node type of the graph
 */
public interface IGraphPathFinder<V> {

    /**
     * Returns an arbitrary path from the source node to the target node (if such exists). If there is no path
     * between them, an empty collection is returned.
     *
     * @param sourceNode the source node of the path
     * @param targetNode the target node of the path
     * @return the path from the source to the target, or empty collection if target is not reachable from source.
     */
    Deque<V> getPath(V sourceNode, V targetNode);

    /**
     * Returns the collection of shortest paths from the source node to the target node (if such exists). If there is no path
     * between them, an empty collection is returned.
     *
     * @param sourceNode the source node of the path
     * @param targetNode the target node of the path
     * @return the collection of shortest paths from the source to the target, or empty collection if target is not reachable from source.
     */
    Iterable<Deque<V>> getShortestPaths(V sourceNode, V targetNode);

    /**
     * Returns the collection of paths from the source node to the target node (if such exists). If there is no path
     * between them, an empty collection is returned.
     *
     * @param sourceNode the source node of the path
     * @param targetNode the target node of the path
     * @return the collection of paths from the source to the target, or empty collection if target is not reachable from source.
     */
    Iterable<Deque<V>> getAllPaths(V sourceNode, V targetNode);

    /**
     * Returns the collection of paths from the source node to any of the target nodes (if such exists). If there is no path
     * between them, an empty collection is returned.
     *
     * @param sourceNode the source node of the path
     * @param targetNodes the set of target nodes of the paths
     * @return the collection of paths from the source to any of the targets, or empty collection if neither target is reachable from source.
     */
    Iterable<Deque<V>> getAllPathsToTargets(V sourceNode, Set<V> targetNodes);


}
