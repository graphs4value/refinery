/*******************************************************************************
 * Copyright (c) 2010-2012, Tamas Szabo, Istvan Rath and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package tools.refinery.interpreter.rete.itc.alg.counting;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import tools.refinery.interpreter.rete.itc.igraph.IBiDirectionalGraphDataSource;
import tools.refinery.interpreter.rete.itc.alg.misc.topsort.TopologicalSorting;
import tools.refinery.interpreter.rete.itc.alg.misc.ITcRelation;
import tools.refinery.interpreter.matchers.util.CollectionsFactory;
import tools.refinery.interpreter.matchers.util.CollectionsFactory.MemoryType;
import tools.refinery.interpreter.matchers.util.IMemoryView;
import tools.refinery.interpreter.matchers.util.IMultiLookup;
import tools.refinery.interpreter.matchers.util.IMultiLookup.ChangeGranularity;

/**
 * Transitive closure relation implementation for the Counting algorithm.
 *
 * @author Tamas Szabo
 *
 * @param <V>
 */
public class CountingTcRelation<V> implements ITcRelation<V> {

    private IMultiLookup<V, V> tuplesForward = null;
    private IMultiLookup<V, V> tuplesBackward = null;

    protected CountingTcRelation(boolean backwardIndexing) {
        tuplesForward = CollectionsFactory.createMultiLookup(Object.class, MemoryType.MULTISETS, Object.class);
        if (backwardIndexing)
            tuplesBackward = CollectionsFactory.createMultiLookup(Object.class, MemoryType.MULTISETS, Object.class);
    }

    protected boolean isEmpty() {
        return 0 == this.tuplesForward.countKeys();
    }

    protected void clear() {
        this.tuplesForward.clear();

        if (tuplesBackward != null) {
            this.tuplesBackward.clear();
        }
    }

    protected void union(CountingTcRelation<V> rA) {
        IMultiLookup<V, V> rForward = rA.tuplesForward;
        for (V source : rForward.distinctKeys()) {
            IMemoryView<V> targetBag = rForward.lookup(source);
            for (V target : targetBag.distinctValues()) {
                this.addTuple(source, target, targetBag.getCount(target));
            }
        }
    }

    public int getCount(V source, V target) {
        IMemoryView<V> bucket = tuplesForward.lookup(source);
        return bucket == null ? 0 : bucket.getCount(target);
    }

    /**
     * Returns true if the tc relation did not contain previously such a tuple that is defined by (source,target), false
     * otherwise (in this case count is incremented with the given count parameter).
     *
     * @param source
     *            the source of the tuple
     * @param target
     *            the target of the tuple
     * @param count
     *            the count of the tuple, must be positive
     * @return true if the relation did not contain previously the tuple
     */
    public boolean addTuple(V source, V target, int count) {
        if (tuplesBackward != null) {
            tuplesBackward.addPairPositiveMultiplicity(target, source, count);
        }

        ChangeGranularity change =
                tuplesForward.addPairPositiveMultiplicity(source, target, count);

        return change != ChangeGranularity.DUPLICATE;
    }

    /**
     * Derivation count of the tuple  (source,target) is incremented or decremented.
     * Returns true iff updated to / from zero derivation count.
     * @since 1.7
     */
    public boolean updateTuple(V source, V target, boolean isInsertion) {
        if (isInsertion) {
            if (tuplesBackward != null) {
                tuplesBackward.addPair(target, source);
            }
            ChangeGranularity change =
                    tuplesForward.addPair(source, target);
            return change != ChangeGranularity.DUPLICATE;
        } else {
            if (tuplesBackward != null) {
                tuplesBackward.removePair(target, source);
            }
            ChangeGranularity change =
                    tuplesForward.removePair(source, target);
            return change != ChangeGranularity.DUPLICATE;
        }
    }

    public void deleteTupleEnd(V deleted) {
        Set<V> sourcesToDelete = CollectionsFactory.createSet();
        Set<V> targetsToDelete = CollectionsFactory.createSet();

        for (V target : tuplesForward.lookupOrEmpty(deleted).distinctValues()) {
            targetsToDelete.add(target);
        }
        if (tuplesBackward != null) {
            for (V source : tuplesBackward.lookupOrEmpty(deleted).distinctValues()) {
                sourcesToDelete.add(source);
            }
        } else {
            for (V sourceCandidate : tuplesForward.distinctKeys()) {
                if (tuplesForward.lookupOrEmpty(sourceCandidate).containsNonZero(deleted))
                    sourcesToDelete.add(sourceCandidate);
            }
        }

        for (V source : sourcesToDelete) {
            int count = tuplesForward.lookupOrEmpty(source).getCount(deleted);
            for (int i=0; i< count; ++i) tuplesForward.removePair(source, deleted);
        }
        for (V target : targetsToDelete) {
            int count = tuplesForward.lookupOrEmpty(deleted).getCount(target);
            for (int i=0; i< count; ++i) tuplesForward.removePair(deleted, target);
        }

        if (tuplesBackward != null) {
            for (V source : sourcesToDelete) {
                int count = tuplesBackward.lookupOrEmpty(deleted).getCount(source);
                for (int i=0; i< count; ++i) tuplesBackward.removePair(deleted, source);
            }
            for (V target : targetsToDelete) {
                int count = tuplesBackward.lookupOrEmpty(target).getCount(deleted);
                for (int i=0; i< count; ++i) tuplesBackward.removePair(target, deleted);
            }
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("TcRelation = ");

        for (V source : tuplesForward.distinctKeys()) {
            IMemoryView<V> targets = tuplesForward.lookup(source);
            for (V target : targets.distinctValues()) {
                sb.append("{(" + source + "," + target + ")," + targets.getCount(target) + "} ");
            }
        }

        return sb.toString();
    }

    @Override
    public Set<V> getTupleEnds(V source) {
        IMemoryView<V> tupEnds = tuplesForward.lookup(source);
        if (tupEnds == null)
            return null;
        return tupEnds.distinctValues();
    }

    /**
     * Returns the set of nodes from which the target node is reachable, if already computed.
     *
     * @param target
     *            the target node
     * @return the set of source nodes
     * @throws UnsupportedOperationException if backwards index not computed
     */
    public Set<V> getTupleStarts(V target) {
        if (tuplesBackward != null) {
            IMemoryView<V> tupStarts = tuplesBackward.lookup(target);
            if (tupStarts == null)
                return null;
            return tupStarts.distinctValues();
        } else {
            throw new UnsupportedOperationException("built without backward indexing");
        }
    }

    @Override
    public Set<V> getTupleStarts() {
        Set<V> nodes = CollectionsFactory.createSet();
        for (V s : tuplesForward.distinctKeys()) {
            nodes.add(s);
        }
        return nodes;
    }

    /**
     * Returns true if a (source, target) node is present in the transitive closure relation, false otherwise.
     *
     * @param source
     *            the source node
     * @param target
     *            the target node
     * @return true if tuple is present, false otherwise
     */
    public boolean containsTuple(V source, V target) {
        return tuplesForward.lookupOrEmpty(source).containsNonZero(target);
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        } else if (obj == null || this.getClass() != obj.getClass()) {
            return false;
        } else {
            CountingTcRelation<V> aTR = (CountingTcRelation<V>) obj;

            return tuplesForward.equals(aTR.tuplesForward);
        }
    }

    @Override
    public int hashCode() {
        return tuplesForward.hashCode();
    }

    public static <V> CountingTcRelation<V> createFrom(IBiDirectionalGraphDataSource<V> gds) {
        List<V> topologicalSorting = TopologicalSorting.compute(gds);
        CountingTcRelation<V> tc = new CountingTcRelation<V>(true);
        Collections.reverse(topologicalSorting);
        for (V n : topologicalSorting) {
            IMemoryView<V> sourceNodes = gds.getSourceNodes(n);
            Set<V> tupEnds = tc.getTupleEnds(n);
            for (V s : sourceNodes.distinctValues()) {
                int count = sourceNodes.getCount(s);
                for (int i = 0; i < count; i++) {
                    tc.updateTuple(s, n, true);
                    if (tupEnds != null) {
                        for (V t : tupEnds) {
                            tc.updateTuple(s, t, true);
                        }
                    }
                }
            }
        }

        return tc;
    }
}
