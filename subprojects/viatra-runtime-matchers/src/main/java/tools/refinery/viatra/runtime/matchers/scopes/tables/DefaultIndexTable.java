/*******************************************************************************
 * Copyright (c) 2010-2018, Gabor Bergmann, IncQuery Labs Ltd.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.viatra.runtime.matchers.scopes.tables;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import tools.refinery.viatra.runtime.matchers.context.IInputKey;
import tools.refinery.viatra.runtime.matchers.memories.MaskedTupleMemory;
import tools.refinery.viatra.runtime.matchers.tuple.ITuple;
import tools.refinery.viatra.runtime.matchers.tuple.Tuple;
import tools.refinery.viatra.runtime.matchers.tuple.TupleMask;
import tools.refinery.viatra.runtime.matchers.util.Accuracy;
import tools.refinery.viatra.runtime.matchers.util.CollectionsFactory;
import tools.refinery.viatra.runtime.matchers.util.CollectionsFactory.MemoryType;
import tools.refinery.viatra.runtime.matchers.util.Direction;
import tools.refinery.viatra.runtime.matchers.util.IMemory;

/**
 * Demo default implementation.
 * <p>
 * <strong>EXPERIMENTAL</strong>. This class or interface has been added as
 * part of a work in progress. There is no guarantee that this API will
 * work or that it will remain the same.
 *
 * @since 2.0
 * @author Gabor Bergmann
 */
public class DefaultIndexTable extends AbstractIndexTable implements ITableWriterGeneric {

    protected IMemory<Tuple> rows = CollectionsFactory.createMultiset(); // TODO use SetMemory if unique
    protected Map<TupleMask, MaskedTupleMemory<?>> indexMemories = CollectionsFactory.createMap();
    private boolean unique;

    /**
     * @param unique
     *            client promises to only insert a given tuple with multiplicity one
     */
    public DefaultIndexTable(IInputKey inputKey, ITableContext tableContext, boolean unique) {
        super(inputKey, tableContext);
        this.unique = unique;
    }

    @Override
    public void write(Direction direction, Tuple row) {
        if (direction == Direction.INSERT) {
            boolean changed = rows.addOne(row);
            if (unique && !changed) {
                String msg = String.format(
                        "Error: trying to add duplicate row %s to the unique table %s. This indicates some errors in underlying model representation.",
                        row, getInputKey().getPrettyPrintableName());
                logError(msg);
            }
            if (changed) {
                for (MaskedTupleMemory<?> indexMemory : indexMemories.values()) {
                    indexMemory.add(row);
                }
                if (emitNotifications) {
                    deliverChangeNotifications(row, true);
                }
            }
        } else { // DELETE
            boolean changed = rows.removeOne(row);
            if (unique && !changed) {
                String msg = String.format(
                        "Error: trying to remove duplicate value %s from the unique table %s. This indicates some errors in underlying model representation.",
                        row, getInputKey().getPrettyPrintableName());
                logError(msg);
            }
            if (changed) {
                for (MaskedTupleMemory<?> indexMemory : indexMemories.values()) {
                    indexMemory.remove(row);
                }
                if (emitNotifications) {
                    deliverChangeNotifications(row, false);
                }
            }
        }
    }

    @Override
    public boolean containsTuple(ITuple seed) {
        return rows.distinctValues().contains(seed);
    }

    private MaskedTupleMemory<?> getIndexMemory(TupleMask seedMask) {
        return indexMemories.computeIfAbsent(seedMask, mask -> {
            MaskedTupleMemory<?> memory = MaskedTupleMemory.create(seedMask, MemoryType.SETS, DefaultIndexTable.this);
            for (Tuple tuple : rows.distinctValues()) {
                memory.add(tuple);
            }
            return memory;
        });
    }
    
    @Override
    public int countTuples(TupleMask seedMask, ITuple seed) {
        switch (seedMask.getSize()) {
        case 0: // unseeded
            return rows.size();
        default:
            return getIndexMemory(seedMask).getOrEmpty(seed).size();
        }
    }

    @Override
    public Optional<Long> estimateProjectionSize(TupleMask groupMask, Accuracy requiredAccuracy) {
        // always exact count
        if (groupMask.getSize() == 0) {
            return rows.size() == 0 ? Optional.of(0L) : Optional.of(1L);
        } else if (groupMask.getSize() == this.emptyTuple.getSize()) {
            return Optional.of((long) rows.size());
        } else { 
            return Optional.of((long)getIndexMemory(groupMask).getKeysetSize());
        }
    }

    @Override
    public Iterable<Tuple> enumerateTuples(TupleMask seedMask, ITuple seed) {
        return getIndexMemory(seedMask).getOrEmpty(seed);
    }
    
    @Override
    public Stream<? extends Tuple> streamTuples(TupleMask seedMask, ITuple seed) {
        return getIndexMemory(seedMask).getOrEmpty(seed).stream();
    }

    @Override
    public Stream<? extends Object> streamValues(TupleMask seedMask, ITuple seed) {
        // we assume there is a single omitted index in the mask
        int queriedColumn = seedMask.getFirstOmittedIndex().getAsInt();
        return getIndexMemory(seedMask).getOrEmpty(seed).stream()
                .map(row2 -> row2.get(queriedColumn));
    }

}
