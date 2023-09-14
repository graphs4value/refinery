/*******************************************************************************
 * Copyright (c) 2010-2018, Gabor Bergmann, IncQuery Labs Ltd.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.viatra.runtime.matchers.memories;

import java.util.Iterator;
import java.util.Map;

import tools.refinery.viatra.runtime.matchers.tuple.ITuple;
import tools.refinery.viatra.runtime.matchers.tuple.Tuple;
import tools.refinery.viatra.runtime.matchers.tuple.TupleMask;
import tools.refinery.viatra.runtime.matchers.util.CollectionsFactory;
import tools.refinery.viatra.runtime.matchers.util.CollectionsFactory.MemoryType;
import tools.refinery.viatra.runtime.matchers.util.timeline.Timeline;
import tools.refinery.viatra.runtime.matchers.util.IMemory;

/**
 * Common parts of nullary and identity specializations.
 * 
 * @noextend This class is not intended to be subclassed by clients.
 * @author Gabor Bergmann
 * @since 2.0
 */
abstract class AbstractTrivialMaskedMemory<Timestamp extends Comparable<Timestamp>> extends MaskedTupleMemory<Timestamp> {

    protected IMemory<Tuple> tuples;

    protected AbstractTrivialMaskedMemory(TupleMask mask, MemoryType bucketType, Object owner) {
        super(mask, owner);
        tuples = CollectionsFactory.createMemory(Object.class, bucketType);
    }
    
    @Override
    public Map<Tuple, Timeline<Timestamp>> getWithTimeline(ITuple signature) {
        throw new UnsupportedOperationException("Timeless memories do not support timestamp-based lookup!");
    }

    @Override
    public void clear() {
        tuples.clear();
    }

    @Override
    public int getTotalSize() {
        return tuples.size();
    }

    @Override
    public Iterator<Tuple> iterator() {
        return tuples.iterator();
    }

}
