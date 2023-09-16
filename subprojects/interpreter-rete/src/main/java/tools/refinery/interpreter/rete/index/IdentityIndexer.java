/*******************************************************************************
 * Copyright (c) 2004-2012 Gabor Bergmann and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package tools.refinery.interpreter.rete.index;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import tools.refinery.interpreter.rete.network.Node;
import tools.refinery.interpreter.rete.network.ReteContainer;
import tools.refinery.interpreter.rete.network.Supplier;
import tools.refinery.interpreter.rete.network.communication.Timestamp;
import tools.refinery.interpreter.matchers.tuple.Tuple;
import tools.refinery.interpreter.matchers.tuple.TupleMask;
import tools.refinery.interpreter.matchers.util.Direction;

/**
 * Defines an abstract trivial indexer that identically projects the contents of some stateful node, and can therefore
 * save space. Can only exist in connection with a stateful store, and must be operated by another node (the active
 * node). Do not attach parents directly!
 *
 * @author Gabor Bergmann
 * @noimplement Rely on the provided implementations
 * @noreference Use only via standard Node and Indexer interfaces
 * @noinstantiate This class is not intended to be instantiated by clients.
 */
public abstract class IdentityIndexer extends SpecializedProjectionIndexer {

    protected abstract Collection<Tuple> getTuples();

    public IdentityIndexer(ReteContainer reteContainer, int tupleWidth, Supplier parent,
                           Node activeNode, List<ListenerSubscription> sharedSubscriptionList) {
        super(reteContainer, TupleMask.identity(tupleWidth), parent, activeNode, sharedSubscriptionList);
    }

    @Override
    public Collection<Tuple> get(Tuple signature) {
        if (contains(signature)) {
            return Collections.singleton(signature);
        } else
            return null;
    }

    protected boolean contains(Tuple signature) {
        return getTuples().contains(signature);
    }

    @Override
    public Collection<Tuple> getSignatures() {
        return getTuples();
    }

    @Override
    public int getBucketCount() {
        return getTuples().size();
    }

    @Override
    public Iterator<Tuple> iterator() {
        return getTuples().iterator();
    }

    @Override
    public void propagateToListener(IndexerListener listener, Direction direction, Tuple updateElement, Timestamp timestamp) {
        listener.notifyIndexerUpdate(direction, updateElement, updateElement, true, timestamp);
    }

}
