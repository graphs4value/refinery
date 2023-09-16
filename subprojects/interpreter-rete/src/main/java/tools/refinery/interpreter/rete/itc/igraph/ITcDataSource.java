/*******************************************************************************
 * Copyright (c) 2010-2012, Tamas Szabo, Istvan Rath and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package tools.refinery.interpreter.rete.itc.igraph;

import java.util.Set;

import tools.refinery.interpreter.rete.itc.alg.misc.IGraphPathFinder;

/**
 * This interface defines those methods that a transitive reachability data source should provide.
 *
 * @author Tamas Szabo
 *
 * @param <V>
 *            the type parameter of the node
 */
public interface ITcDataSource<V> {

    /**
     * Attach a transitive closure relation observer.
     *
     * @param to
     *            the observer object
     */
    public void attachObserver(ITcObserver<V> to);

    /**
     * Detach a transitive closure relation observer.
     *
     * @param to
     *            the observer object
     */
    public void detachObserver(ITcObserver<V> to);

    /**
     * Returns all nodes which are reachable from the source node.
     *
     * @param source
     *            the source node
     * @return the set of target nodes
     */
    public Set<V> getAllReachableTargets(V source);

    /**
     * Returns all nodes from which the target node is reachable.
     *
     * @param target
     *            the target node
     * @return the set of source nodes
     */
    public Set<V> getAllReachableSources(V target);

    /**
     * Returns true if the target node is reachable from the source node.
     *
     * @param source
     *            the source node
     * @param target
     *            the target node
     * @return true if target is reachable from source, false otherwise
     */
    public boolean isReachable(V source, V target);

    /**
     * The returned {@link IGraphPathFinder} can be used to retrieve paths between nodes using transitive reachability.
     *
     * @return a path finder for the graph.
     */
    public IGraphPathFinder<V> getPathFinder();

    /**
     * Call this method to properly dispose the data structures of a transitive closure algorithm.
     */
    public void dispose();
}
