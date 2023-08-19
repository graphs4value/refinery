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
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import tools.refinery.viatra.runtime.matchers.context.IInputKey;
import tools.refinery.viatra.runtime.matchers.context.IQueryRuntimeContextListener;
import tools.refinery.viatra.runtime.matchers.tuple.ITuple;
import tools.refinery.viatra.runtime.matchers.tuple.Tuple;
import tools.refinery.viatra.runtime.matchers.tuple.TupleMask;
import tools.refinery.viatra.runtime.matchers.tuple.Tuples;
import tools.refinery.viatra.runtime.matchers.util.Accuracy;
import tools.refinery.viatra.runtime.matchers.util.CollectionsFactory;

/**
 * Disjoint union of the provided child tables.
 * 
 * Used e.g. to present a transitive instance table as a view composed from direct instance tables.
 * <p>
 * <strong>EXPERIMENTAL</strong>. This class or interface has been added as
 * part of a work in progress. There is no guarantee that this API will
 * work or that it will remain the same.
 *
 * @since 2.0
 * @author Gabor Bergmann
 */
public class DisjointUnionTable extends AbstractIndexTable {

    protected List<IIndexTable> childTables = CollectionsFactory.createObserverList();

    public DisjointUnionTable(IInputKey inputKey, ITableContext tableContext) {
        super(inputKey, tableContext);
    }

    public List<IIndexTable> getChildTables() {
        return Collections.unmodifiableList(childTables);
    }

    /**
     * Precondition: the new child currently is, and will forever stay, disjoint from any other child tables.
     */
    public void addChildTable(IIndexTable child) {
        if (getInputKey().getArity() != child.getInputKey().getArity())
            throw new IllegalArgumentException(child.toString());

        childTables.add(child);
        
        if (emitNotifications) {
            for (Tuple tuple : child.enumerateTuples(emptyMask, Tuples.staticArityFlatTupleOf())) {
                deliverChangeNotifications(tuple, true);
            }
        }
    }

    @Override
    public int countTuples(TupleMask seedMask, ITuple seed) {
        int count = 0;
        for (IIndexTable child : childTables) {
            count += child.countTuples(seedMask, seed);
        }
        return count;
    }


    @Override
    public Optional<Long> estimateProjectionSize(TupleMask groupMask, Accuracy requiredAccuracy) {
        // exact results for trivial projections
        if (groupMask.getSize() == 0) {
            for (IIndexTable child : childTables) {
                if (0 != child.countTuples(this.emptyMask, Tuples.staticArityFlatTupleOf()))
                    return Optional.of(1L);
            }
            return Optional.of(0L);
        } else if (groupMask.getSize() == emptyTuple.getSize()) {
            return Optional.of((long)countTuples(this.emptyMask, Tuples.staticArityFlatTupleOf()));
        }
        // summing child tables is an upper bound
        if (Accuracy.BEST_UPPER_BOUND.atLeastAsPreciseAs(requiredAccuracy)) {
            return Optional.of((long)countTuples(this.emptyMask, Tuples.staticArityFlatTupleOf()));
        } else { // (Accuracy.BEST_LOWER_BOUND == requiredAccuracy)  
            //projections of child tables may coincide, but the largest one is still a lower bound
            Optional<Long> maxProjection = Optional.empty();
            for (IIndexTable child : childTables) {
                Optional<Long> estimateOfChild = child.estimateProjectionSize(groupMask, requiredAccuracy);
                if (estimateOfChild.isPresent()) {
                    maxProjection = Optional.of(Math.max(estimateOfChild.get(), maxProjection.orElse(0L)));
                }
            }
            return maxProjection;
        } 
    }
    
    @Override
    public Stream<? extends Tuple> streamTuples(TupleMask seedMask, ITuple seed) {
        Stream<? extends Tuple> stream = Stream.empty();
        for (IIndexTable child : childTables) {
            Stream<? extends Tuple> childStream = child.streamTuples(seedMask, seed);
            stream = Stream.concat(stream, childStream);
        }
        return stream;
    }

    @Override
    public Stream<? extends Object> streamValues(TupleMask seedMask, ITuple seed) {
        Stream<? extends Object> stream = Stream.empty();
        for (IIndexTable child : childTables) {
            Stream<? extends Object> childStream = child.streamValues(seedMask, seed);
            stream = Stream.concat(stream, childStream);
        }
        return stream;
    }

    @Override
    public boolean containsTuple(ITuple seed) {
        for (IIndexTable child : childTables) {
            if (child.containsTuple(seed))
                return true;
        }
        return false;
    }
    
    @Override
    public void addUpdateListener(Tuple seed, IQueryRuntimeContextListener listener) {
        super.addUpdateListener(seed, listener);
        
        for (IIndexTable table : childTables) {
            table.addUpdateListener(seed, new ListenerWrapper(listener));
        }
    }
    @Override
    public void removeUpdateListener(Tuple seed, IQueryRuntimeContextListener listener) {
        super.removeUpdateListener(seed, listener);
        
        for (IIndexTable table : childTables) {
            table.removeUpdateListener(seed, new ListenerWrapper(listener));
        }
    }
    
    
    // TODO this would not be necessary 
    // if we moved from IQRCL to an interface that does not expose the input key
    private class ListenerWrapper implements IQueryRuntimeContextListener {

        private IQueryRuntimeContextListener wrappedListener;
        public ListenerWrapper(IQueryRuntimeContextListener wrappedListener) {
            this.wrappedListener = wrappedListener;
        }
        
        @Override
        public void update(IInputKey key, Tuple updateTuple, boolean isInsertion) {
            wrappedListener.update(getInputKey(), updateTuple, isInsertion);
        }
        
        @Override
        public int hashCode() {
            return Objects.hash(wrappedListener, DisjointUnionTable.this);
        }
        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            ListenerWrapper other = (ListenerWrapper) obj;
            if (!getOuterType().equals(other.getOuterType()))
                return false;
            return Objects.equals(wrappedListener, other.wrappedListener);
        }
        private DisjointUnionTable getOuterType() {
            return DisjointUnionTable.this;
        }
        @Override
        public String toString() {
            return "Wrapper to DisjointUnion("+getInputKey().getPrettyPrintableName()+") for " + wrappedListener;
        }
    }

}
