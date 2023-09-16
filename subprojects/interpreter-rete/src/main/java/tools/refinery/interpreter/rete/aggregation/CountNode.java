/*******************************************************************************
 * Copyright (c) 2004-2009 Gabor Bergmann and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package tools.refinery.interpreter.rete.aggregation;

import java.util.Collection;

import tools.refinery.interpreter.matchers.tuple.Tuple;
import tools.refinery.interpreter.rete.network.ReteContainer;

/**
 * An aggregation node that simply counts the number of tuples conforming to the signature.
 *
 * @author Gabor Bergmann
 * @since 1.4
 */
public class CountNode extends IndexerBasedAggregatorNode {

    public CountNode(ReteContainer reteContainer) {
        super(reteContainer);
    }

    int sizeOf(Collection<Tuple> group) {
        return group == null ? 0 : group.size();
    }

    @Override
    public Object aggregateGroup(Tuple signature, Collection<Tuple> group) {
        return sizeOf(group);
    }

}
