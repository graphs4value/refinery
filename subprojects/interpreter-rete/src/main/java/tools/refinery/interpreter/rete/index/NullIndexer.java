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
import tools.refinery.interpreter.matchers.tuple.Tuples;
import tools.refinery.interpreter.matchers.util.Direction;

/**
 * Defines an abstract trivial indexer that projects the contents of some stateful node to the empty tuple, and can
 * therefore save space. Can only exist in connection with a stateful store, and must be operated by another node (the
 * active node). Do not attach parents directly!
 *
 * @author Gabor Bergmann
 * @noimplement Rely on the provided implementations
 * @noreference Use only via standard Node and Indexer interfaces
 * @noinstantiate This class is not intended to be instantiated by clients.
 */
public abstract class NullIndexer extends SpecializedProjectionIndexer {

    protected abstract Collection<Tuple> getTuples();

    protected static final Tuple nullSignature = Tuples.staticArityFlatTupleOf();
    protected static final Collection<Tuple> nullSingleton = Collections.singleton(nullSignature);
    protected static final Collection<Tuple> emptySet = Collections.emptySet();

    public NullIndexer(ReteContainer reteContainer, int tupleWidth, Supplier parent, Node activeNode,
                       List<ListenerSubscription> sharedSubscriptionList) {
        super(reteContainer, TupleMask.linear(0, tupleWidth), parent, activeNode, sharedSubscriptionList);
    }

    @Override
    public Collection<Tuple> get(Tuple signature) {
        if (nullSignature.equals(signature))
            return isEmpty() ? null : getTuples();
        else
            return null;
    }

    @Override
    public Collection<Tuple> getSignatures() {
        return isEmpty() ? emptySet : nullSingleton;
    }

    protected boolean isEmpty() {
        return getTuples().isEmpty();
    }

    protected boolean isSingleElement() {
        return getTuples().size() == 1;
    }

    @Override
    public Iterator<Tuple> iterator() {
        return getTuples().iterator();
    }

    @Override
    public int getBucketCount() {
        return getTuples().isEmpty() ? 0 : 1;
    }

    @Override
    public void propagateToListener(IndexerListener listener, Direction direction, Tuple updateElement,
            Timestamp timestamp) {
        boolean radical = (direction == Direction.DELETE && isEmpty())
                || (direction == Direction.INSERT && isSingleElement());
        listener.notifyIndexerUpdate(direction, updateElement, nullSignature, radical, timestamp);
    }

}
