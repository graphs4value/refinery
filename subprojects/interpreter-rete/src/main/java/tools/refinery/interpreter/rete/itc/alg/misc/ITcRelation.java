/*******************************************************************************
 * Copyright (c) 2010-2012, Tamas Szabo, Istvan Rath and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package tools.refinery.interpreter.rete.itc.alg.misc;

import java.util.Set;

public interface ITcRelation<V> {

    /**
     * Returns the starting nodes from a transitive closure relation.
     *
     * @return the set of starting nodes
     */
    public Set<V> getTupleStarts();

    /**
     * Returns the set of nodes that are reachable from the given node.
     *
     * @param start
     *            the starting node
     * @return the set of reachable nodes
     */
    public Set<V> getTupleEnds(V start);
}
