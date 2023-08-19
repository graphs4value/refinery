/*******************************************************************************
 * Copyright (c) 2010-2018, Gabor Bergmann, IncQuery Labs Ltd.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.viatra.runtime.matchers.scopes.tables;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import tools.refinery.viatra.runtime.matchers.context.IInputKey;
import tools.refinery.viatra.runtime.matchers.tuple.ITuple;
import tools.refinery.viatra.runtime.matchers.tuple.Tuple;
import tools.refinery.viatra.runtime.matchers.tuple.TupleMask;
import tools.refinery.viatra.runtime.matchers.tuple.Tuples;
import tools.refinery.viatra.runtime.matchers.util.Accuracy;
import tools.refinery.viatra.runtime.matchers.util.CollectionsFactory;
import tools.refinery.viatra.runtime.matchers.util.CollectionsFactory.MemoryType;
import tools.refinery.viatra.runtime.matchers.util.Direction;
import tools.refinery.viatra.runtime.matchers.util.IMemoryView;
import tools.refinery.viatra.runtime.matchers.util.IMultiLookup;
import tools.refinery.viatra.runtime.matchers.util.IMultiLookup.ChangeGranularity;

/**
 * Simple source-target bidirectional mapping.
 * 
 * <p>
 * TODO: specialize for to-one features and unique to-many features
 * <p>
 * TODO: on-demand construction of valueToHolderMap
 * <p>
 * TODO: support for lean indexing, opposites, long surrogate ids, etc.
 * <p>
 * <strong>EXPERIMENTAL</strong>. This class or interface has been added as
 * part of a work in progress. There is no guarantee that this API will
 * work or that it will remain the same.
 *
 * @since 2.0
 * @author Gabor Bergmann
 */
public class SimpleBinaryTable<Source, Target> extends AbstractIndexTable
        implements ITableWriterBinary.Table<Source, Target> {

    /**
     * value -> holder(s)
     * <p>
     * this is currently the primary store, may hold duplicates depending on the unique parameter
     */
    private IMultiLookup<Target, Source> valueToHolderMap;
    /**
     * holder -> value(s); constructed on-demand, null if unused
     */
    private IMultiLookup<Source, Target> holderToValueMap;
    private int totalRowCount = 0;
    private boolean unique;

    /**
     * @param unique
     *            client promises to only insert a given tuple with multiplicity one
     */
    public SimpleBinaryTable(IInputKey inputKey, ITableContext tableContext, boolean unique) {
        super(inputKey, tableContext);
        this.unique = unique;
        valueToHolderMap = CollectionsFactory.createMultiLookup(Object.class,
                unique ? MemoryType.SETS : MemoryType.MULTISETS, Object.class);
        if (2 != inputKey.getArity())
            throw new IllegalArgumentException(inputKey.toString());
    }

    @Override
    public void write(Direction direction, Source holder, Target value) {
        if (direction == Direction.INSERT) {
            try {
                // TODO we currently assume V2H map exists
                boolean changed = addToValueToHolderMap(value, holder);
                if (holderToValueMap != null) {
                    addToHolderToValueMap(value, holder);
                }
                if (changed) {
                    totalRowCount++;
                    if (emitNotifications) {
                        deliverChangeNotifications(Tuples.staticArityFlatTupleOf(holder, value), true);
                    }
                }
            } catch (IllegalStateException ex) { // if unique table and duplicate tuple
                String msg = String.format(
                        "Error: trying to add duplicate value %s to the unique feature %s of host object %s. This indicates some errors in underlying model representation.",
                        value, getInputKey().getPrettyPrintableName(), holder);
                logError(msg);
            }
        } else { // DELETE
            try {
                // TODO we currently assume V2H map exists
                boolean changed = removeFromValueToHolderMap(value, holder);
                if (holderToValueMap != null) {
                    removeFromHolderToValueMap(value, holder);
                }
                if (changed) {
                    totalRowCount--;
                    if (emitNotifications) {
                        deliverChangeNotifications(Tuples.staticArityFlatTupleOf(holder, value), false);
                    }
                }
            } catch (IllegalStateException ex) { // if unique table and duplicate tuple
                String msg = String.format(
                        "Error: trying to remove non-existing value %s from the feature %s of host object %s. This indicates some errors in underlying model representation.",
                        value, getInputKey().getPrettyPrintableName(), holder);
                logError(msg);
            }
        }
    }

    private boolean addToHolderToValueMap(Target value, Source holder) {
        return ChangeGranularity.DUPLICATE != holderToValueMap.addPair(holder, value);
    }

    private boolean addToValueToHolderMap(Target value, Source holder) {
        return ChangeGranularity.DUPLICATE != valueToHolderMap.addPair(value, holder);
    }

    /**
     * @throws IllegalStateException
     */
    private boolean removeFromHolderToValueMap(Target value, Source holder) {
        return ChangeGranularity.DUPLICATE != holderToValueMap.removePair(holder, value);
    }

    /**
     * @throws IllegalStateException
     */
    private boolean removeFromValueToHolderMap(Target value, Source holder) {
        return ChangeGranularity.DUPLICATE != valueToHolderMap.removePair(value, holder);
    }

    @SuppressWarnings("unchecked")
    @Override
    public int countTuples(TupleMask seedMask, ITuple seed) {
        switch (seedMask.getSize()) {
        case 0: // unseeded
            // TODO we currently assume V2H map exists
            return totalRowCount;
        case 1: // lookup by source or target
            int seedIndex = seedMask.indices[0];
            if (seedIndex == 0) { // lookup by source
                Source source = (Source) seed.get(0);
                return getDistinctValuesOfHolder(source).size();
            } else if (seedIndex == 1) { // lookup by target
                Target target = (Target) seed.get(0);
                return getDistinctHoldersOfValue(target).size();
            } else
                throw new IllegalArgumentException(seedMask.toString());
        case 2: // containment check
            // hack: if mask is not identity, then it is [1,0]/2, which is its own inverse
            Source source = (Source) seedMask.getValue(seed, 0);
            Target target = (Target) seedMask.getValue(seed, 1);
            if (containsRow(source, target))
                return 1;
            else
                return 0;
        default:
            throw new IllegalArgumentException(seedMask.toString());
        }
    }

    @Override
    public Optional<Long> estimateProjectionSize(TupleMask groupMask, Accuracy requiredAccuracy) {
        // always exact count
        if (groupMask.getSize() == 0) {
            return totalRowCount == 0 ? Optional.of(0L) : Optional.of(1L);
        } else if (groupMask.getSize() == 2) {
            return Optional.of((long)totalRowCount);
        } else if (groupMask.indices[0] == 0) { // project to holder
            return Optional.of((long)getHolderToValueMap().countKeys());
        } else { // (groupMask.indices[0] == 0) // project to value 
            return Optional.of((long)getValueToHolderMap().countKeys());
        }
    }

    @Override
    public Stream<? extends Tuple> streamTuples(TupleMask seedMask, ITuple seed) {
        switch (seedMask.getSize()) {
        case 0: // unseeded
            // TODO we currently assume V2H map exists
            return getAllDistinctValuesStream()
                    .flatMap(value -> valueToHolderMap.lookup(value).distinctValues().stream()
                    .map(source -> Tuples.staticArityFlatTupleOf(source, value)));

        case 1: // lookup by source or target
            int seedIndex = seedMask.indices[0];
            if (seedIndex == 0) { // lookup by source
                @SuppressWarnings("unchecked")
                Source source = (Source) seed.get(0);
                return getDistinctValuesOfHolder(source).stream()
                        .map(target -> Tuples.staticArityFlatTupleOf(source, target));
            } else if (seedIndex == 1) { // lookup by target
                @SuppressWarnings("unchecked")
                Target target = (Target) seed.get(0);
                return getDistinctHoldersOfValue(target).stream()
                            .map(source -> Tuples.staticArityFlatTupleOf(source, target));
            } else
                throw new IllegalArgumentException(seedMask.toString());
        case 2: // containment check
            // hack: if mask is not identity, then it is [1,0]/2, which is its own inverse
            @SuppressWarnings("unchecked")
            Source source = (Source) seedMask.getValue(seed, 0);
            @SuppressWarnings("unchecked")
            Target target = (Target) seedMask.getValue(seed, 1);

            if (containsRow(source, target))
                return Stream.of(Tuples.staticArityFlatTupleOf(source, target));
            else
                return Stream.empty();
        default:
            throw new IllegalArgumentException(seedMask.toString());
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public Iterable<? extends Object> enumerateValues(TupleMask seedMask, ITuple seed) {
        if (seedMask.getSize() != 1)
            throw new IllegalArgumentException(seedMask.toString());

        int seedIndex = seedMask.indices[0];
        if (seedIndex == 0) { // lookup by source
            return getDistinctValuesOfHolder((Source) seed.get(0));
        } else if (seedIndex == 1) { // lookup by target
            return getDistinctHoldersOfValue((Target) seed.get(0));
        } else
            throw new IllegalArgumentException(seedMask.toString());

    }
    @Override
    public Stream<? extends Object> streamValues(TupleMask seedMask, ITuple seed) {
        if (seedMask.getSize() != 1)
            throw new IllegalArgumentException(seedMask.toString());

        int seedIndex = seedMask.indices[0];
        if (seedIndex == 0) { // lookup by source
            return getDistinctValuesOfHolder((Source) seed.get(0)).stream();
        } else if (seedIndex == 1) { // lookup by target
            return getDistinctHoldersOfValue((Target) seed.get(0)).stream();
        } else
            throw new IllegalArgumentException(seedMask.toString());

    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean containsTuple(ITuple seed) {
        return containsRow((Source) seed.get(0), (Target) seed.get(1));
    }

    public boolean containsRow(Source source, Target target) {
        // TODO we currently assume V2H map exists
        if (valueToHolderMap != null) {
            IMemoryView<Source> holders = valueToHolderMap.lookup(target);
            return holders != null && holders.containsNonZero(source);
        } else
            throw new UnsupportedOperationException("TODO implement");
    }

    public Iterable<Source> getAllDistinctHolders() {
        return getHolderToValueMap().distinctKeys();
    }
    public Stream<Source> getAllDistinctHoldersStream() {
        return getHolderToValueMap().distinctKeysStream();
    }
    public Iterable<Target> getAllDistinctValues() {
        return getValueToHolderMap().distinctKeys();
    }
    public Stream<Target> getAllDistinctValuesStream() {
        return getValueToHolderMap().distinctKeysStream();
    }

    public Set<Source> getDistinctHoldersOfValue(Target value) {
        IMemoryView<Source> holdersMultiset = getValueToHolderMap().lookup(value);
        if (holdersMultiset == null)
            return Collections.emptySet();
        else
            return holdersMultiset.distinctValues();
    }

    public Set<Target> getDistinctValuesOfHolder(Source holder) {
        IMemoryView<Target> valuesMultiset = getHolderToValueMap().lookup(holder);
        if (valuesMultiset == null)
            return Collections.emptySet();
        else
            return valuesMultiset.distinctValues();
    }

    private IMultiLookup<Source, Target> getHolderToValueMap() {
        if (holderToValueMap == null) {
            holderToValueMap = CollectionsFactory.createMultiLookup(Object.class, MemoryType.SETS, // no duplicates, as
                                                                                                   // this is the
                                                                                                   // secondary
                                                                                                   // collection
                    Object.class);

            // TODO we currently assume V2H map exists
            for (Target value : valueToHolderMap.distinctKeys()) {
                for (Source holder : valueToHolderMap.lookup(value).distinctValues()) {
                    holderToValueMap.addPair(holder, value);
                }
            }
        }
        return holderToValueMap;
    }

    private IMultiLookup<Target, Source> getValueToHolderMap() {
        // TODO we currently assume V2H map exists
        return valueToHolderMap;
    }

}
