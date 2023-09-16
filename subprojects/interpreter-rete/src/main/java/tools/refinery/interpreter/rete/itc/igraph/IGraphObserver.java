/*******************************************************************************
 * Copyright (c) 2010-2012, Tamas Szabo, Istvan Rath and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package tools.refinery.interpreter.rete.itc.igraph;

/**
 * Interface GraphObserver is used to observ the changes in a graph; edge and node insertion/deleteion.
 *
 * @author Tamas Szabo
 *
 */
public interface IGraphObserver<V> {

    /**
     * Used to notify when an edge is inserted into the graph.
     *
     * @param source
     *            the source of the edge
     * @param target
     *            the target of the edge
     */
    public void edgeInserted(V source, V target);

    /**
     * Used to notify when an edge is deleted from the graph.
     *
     * @param source
     *            the source of the edge
     * @param target
     *            the target of the edge
     */
    public void edgeDeleted(V source, V target);

    /**
     * Used to notify when a node is inserted into the graph.
     *
     * @param n
     *            the node
     */
    public void nodeInserted(V n);

    /**
     * Used to notify when a node is deleted from the graph.
     *
     * @param n
     *            the node
     */
    public void nodeDeleted(V n);
}
