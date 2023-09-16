/*******************************************************************************
 * Copyright (c) 2010-2012, Tamas Szabo, Istvan Rath and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package tools.refinery.interpreter.rete.itc.igraph;

import tools.refinery.interpreter.matchers.util.IMemoryView;
import tools.refinery.interpreter.matchers.util.IMultiset;

/**
 * A bi-directional graph data source supports all operations that an {@link IGraphDataSource} does, but it
 * also makes it possible to query the incoming edges of nodes, not only the outgoing edges.
 *
 * @author Tamas Szabo
 *
 * @param <V> the type of the nodes in the graph
 */
public interface IBiDirectionalGraphDataSource<V> extends IGraphDataSource<V> {

    /**
     * Returns the source nodes for the given target node.
     * The returned data structure is an {@link IMultiset} because of potential parallel edges in the graph data source.
     *
     * The method must not return null.
     *
     * @param target the target node
     * @return the multiset of source nodes
     * @since 2.0
     */
    public IMemoryView<V> getSourceNodes(V target);

}
