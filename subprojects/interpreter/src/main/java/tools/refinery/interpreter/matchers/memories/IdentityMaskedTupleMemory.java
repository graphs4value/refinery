/*******************************************************************************
 * Copyright (c) 2010-2018, Gabor Bergmann, IncQuery Labs Ltd.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.interpreter.matchers.memories;

import java.util.Collection;
import java.util.Collections;

import tools.refinery.interpreter.matchers.tuple.ITuple;
import tools.refinery.interpreter.matchers.tuple.Tuple;
import tools.refinery.interpreter.matchers.tuple.TupleMask;
import tools.refinery.interpreter.matchers.util.CollectionsFactory.MemoryType;

/**
 * Specialized for identity mask; tuples are stored as a simple set/multiset memory.
 *
 * @author Gabor Bergmann
 * @since 2.0
 */
public final class IdentityMaskedTupleMemory<Timestamp extends Comparable<Timestamp>> extends AbstractTrivialMaskedMemory<Timestamp> {

    /**
     * @param mask
     *            The mask used to index the matchings
     * @param owner the object "owning" this memory
     * @param bucketType the kind of tuple collection maintained for each indexer bucket
     * @since 2.0
     */
    public IdentityMaskedTupleMemory(TupleMask mask, MemoryType bucketType, Object owner) {
        super(mask, bucketType, owner);
        if (!mask.isIdentity()) throw new IllegalArgumentException(mask.toString());
    }

    @Override
    public int getKeysetSize() {
        return tuples.size();
    }

    @Override
    public Iterable<Tuple> getSignatures() {
        return tuples;
    }

    @Override
    public Collection<Tuple> get(ITuple signature) {
        Tuple contained = tuples.theContainedVersionOfUnsafe(signature);
        return contained != null ?
                        Collections.singleton(contained) :
                        null;
    }

    @Override
    public boolean remove(Tuple tuple, Tuple signature) {
        return tuples.removeOne(tuple);
    }

    @Override
    public boolean remove(Tuple tuple) {
        return tuples.removeOne(tuple);
    }

    @Override
    public boolean add(Tuple tuple, Tuple signature) {
        return tuples.addOne(tuple);
    }

    @Override
    public boolean add(Tuple tuple) {
        return tuples.addOne(tuple);
    }

}
