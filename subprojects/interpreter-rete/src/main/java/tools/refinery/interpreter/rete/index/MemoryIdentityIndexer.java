/*******************************************************************************
 * Copyright (c) 2004-2008 Gabor Bergmann and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package tools.refinery.interpreter.rete.index;

import java.util.Collection;
import java.util.List;

import tools.refinery.interpreter.rete.network.Receiver;
import tools.refinery.interpreter.rete.network.ReteContainer;
import tools.refinery.interpreter.rete.network.Supplier;
import tools.refinery.interpreter.matchers.tuple.Tuple;

/**
 * Defines a trivial indexer that identically projects the contents of a memory-equipped node, and can therefore save
 * space. Can only exist in connection with a memory, and must be operated by another node. Do not attach parents
 * directly!
 *
 * @noimplement Rely on the provided implementations
 * @noreference Use only via standard Node and Indexer interfaces
 * @noinstantiate This class is not intended to be instantiated by clients.
 * @author Gabor Bergmann
 */

public class MemoryIdentityIndexer extends IdentityIndexer {

    protected final Collection<Tuple> memory;

    /**
     * @param reteContainer
     * @param tupleWidth
     *            the width of the tuples of memoryNode
     * @param memory
     *            the memory whose contents are to be identity-indexed
     * @param parent
     *            the parent node that owns the memory
     */
    public MemoryIdentityIndexer(ReteContainer reteContainer, int tupleWidth, Collection<Tuple> memory,
                                 Supplier parent, Receiver activeNode, List<ListenerSubscription> sharedSubscriptionList) {
        super(reteContainer, tupleWidth, parent, activeNode, sharedSubscriptionList);
        this.memory = memory;
    }

    @Override
    protected Collection<Tuple> getTuples() {
        return this.memory;
    }

}
