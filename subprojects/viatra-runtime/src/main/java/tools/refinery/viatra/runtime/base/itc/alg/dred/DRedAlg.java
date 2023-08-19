/*******************************************************************************
 * Copyright (c) 2010-2012, Tamas Szabo, Istvan Rath and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package tools.refinery.viatra.runtime.base.itc.alg.dred;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import tools.refinery.viatra.runtime.base.itc.alg.misc.DFSPathFinder;
import tools.refinery.viatra.runtime.base.itc.alg.misc.IGraphPathFinder;
import tools.refinery.viatra.runtime.base.itc.alg.misc.Tuple;
import tools.refinery.viatra.runtime.base.itc.alg.misc.dfs.DFSAlg;
import tools.refinery.viatra.runtime.base.itc.igraph.IGraphDataSource;
import tools.refinery.viatra.runtime.base.itc.igraph.IGraphObserver;
import tools.refinery.viatra.runtime.base.itc.igraph.ITcDataSource;
import tools.refinery.viatra.runtime.base.itc.igraph.ITcObserver;
import tools.refinery.viatra.runtime.matchers.util.IMemoryView;

/**
 * This class is the optimized implementation of the DRED algorithm.
 * 
 * @author Tamas Szabo
 * 
 * @param <V>
 *            the type parameter of the nodes in the graph data source
 */
public class DRedAlg<V> implements IGraphObserver<V>, ITcDataSource<V> {

    private IGraphDataSource<V> graphDataSource = null;
    private DRedTcRelation<V> tc = null;
    private DRedTcRelation<V> dtc = null;
    private List<ITcObserver<V>> observers;

    /**
     * Constructs a new DRED algorithm and initializes the transitive closure relation with the given graph data source.
     * Attach itself on the graph data source as an observer.
     * 
     * @param gds
     *            the graph data source instance
     */
    public DRedAlg(IGraphDataSource<V> gds) {
        this.observers = new ArrayList<ITcObserver<V>>();
        this.graphDataSource = gds;
        this.tc = new DRedTcRelation<V>();
        this.dtc = new DRedTcRelation<V>();
        initTc();
        graphDataSource.attachObserver(this);
    }

    /**
     * Constructs a new DRED algorithm and initializes the transitive closure relation with the given relation. Attach
     * itself on the graph data source as an observer.
     * 
     * @param gds
     *            the graph data source instance
     * @param tc
     *            the transitive closure instance
     */
    public DRedAlg(IGraphDataSource<V> gds, DRedTcRelation<V> tc) {
        this.graphDataSource = gds;
        this.tc = tc;
        this.dtc = new DRedTcRelation<V>();
        graphDataSource.attachObserver(this);
    }

    /**
     * Initializes the transitive closure relation.
     */
    private void initTc() {
        DFSAlg<V> dfsa = new DFSAlg<V>(this.graphDataSource);
        this.setTcRelation(dfsa.getTcRelation());
        this.graphDataSource.detachObserver(dfsa);
    }

    @Override
    public void edgeInserted(V source, V target) {
        if (!source.equals(target)) {
            Set<V> tupStarts = null;
            Set<V> tupEnds = null;
            Set<Tuple<V>> tuples = new HashSet<Tuple<V>>();

            if (!source.equals(target) && tc.addTuple(source, target)) {
                tuples.add(new Tuple<V>(source, target));
            }

            // 2. d+(tc(x,y)) :- d+(tc(x,z)) & lv(z,y) Descartes product
            tupStarts = tc.getTupleStarts(source);
            tupEnds = tc.getTupleEnds(target);

            for (V s : tupStarts) {
                for (V t : tupEnds) {
                    if (!s.equals(t) && tc.addTuple(s, t)) {
                        tuples.add(new Tuple<V>(s, t));
                    }
                }
            }

            // (s, source) -> (source, target)
            // tupStarts = tc.getTupleStarts(source);
            for (V s : tupStarts) {
                if (!s.equals(target) && tc.addTuple(s, target)) {
                    tuples.add(new Tuple<V>(s, target));
                }
            }

            // (source, target) -> (target, t)
            // tupEnds = tc.getTupleEnds(target);
            for (V t : tupEnds) {
                if (!source.equals(t) && tc.addTuple(source, t)) {
                    tuples.add(new Tuple<V>(source, t));
                }
            }

            notifyTcObservers(tuples, 1);
        }
    }

    @Override
    public void edgeDeleted(V source, V target) {
        if (!source.equals(target)) {

            // Computing overestimate, Descartes product of A and B sets, where
            // A: those nodes from which source is reachable
            // B: those nodes which is reachable from target

            Map<Tuple<V>, Integer> tuples = new HashMap<Tuple<V>, Integer>();
            Set<V> sources = tc.getTupleStarts(source);
            Set<V> targets = tc.getTupleEnds(target);

            tc.removeTuple(source, target);
            tuples.put(new Tuple<V>(source, target), -1);

            for (V s : sources) {
                for (V t : targets) {
                    if (!s.equals(t)) {
                        tc.removeTuple(s, t);
                        tuples.put(new Tuple<V>(s, t), -1);
                    }
                }
            }

            for (V s : sources) {
                if (!s.equals(target)) {
                    tc.removeTuple(s, target);
                    tuples.put(new Tuple<V>(s, target), -1);
                }
            }

            for (V t : targets) {
                if (!source.equals(t)) {
                    tc.removeTuple(source, t);
                    tuples.put(new Tuple<V>(source, t), -1);
                }
            }

            // System.out.println("overestimate: "+dtc);

            // Modify overestimate with those tuples that have alternative derivations
            // 1. q+(tc(x,y)) :- lv(x,y)
            for (V s : graphDataSource.getAllNodes()) {
                IMemoryView<V> targetNodes = graphDataSource.getTargetNodes(s);
                for (Entry<V, Integer> entry : targetNodes.entriesWithMultiplicities()) {
                    for (int i = 0; i < entry.getValue(); i++) {
                        V t = entry.getKey();
                        if (!s.equals(t)) {
                            tc.addTuple(s, t);
                            Tuple<V> tuple = new Tuple<V>(s, t);
                            Integer count = tuples.get(tuple);
                            if (count != null && count == -1) {
                                tuples.remove(tuple);
                            }
                        }
                        
                    }
                }
            }

            // 2. q+(tc(x,y)) :- tcv(x,z) & lv(z,y)
            DRedTcRelation<V> newTups = new DRedTcRelation<V>();
            dtc.clear();
            dtc.union(tc);

            while (!dtc.isEmpty()) {

                newTups.clear();
                newTups.union(dtc);
                dtc.clear();

                for (V s : newTups.getTupleStarts()) {
                    for (V t : newTups.getTupleEnds(s)) {
                        IMemoryView<V> targetNodes = graphDataSource.getTargetNodes(t);
                        if (targetNodes != null) {
                            for (Entry<V, Integer> entry : targetNodes.entriesWithMultiplicities()) {
                                for (int i = 0; i < entry.getValue(); i++) {
                                    V tn = entry.getKey();
                                    if (!s.equals(tn) && tc.addTuple(s, tn)) {
                                        dtc.addTuple(s, tn);
                                        tuples.remove(new Tuple<V>(s, tn));
                                    }
                                }
                            }
                        }
                    }
                }
            }

            notifyTcObservers(tuples.keySet(), -1);
        }
    }

    @Override
    public void nodeInserted(V n) {
        // Node inserted does not result new tc tuple.
    }

    @Override
    public void nodeDeleted(V n) {
        // FIXME node deletion may involve the deletion of incoming and outgoing edges too
        Set<V> set = tc.getTupleEnds(n);
        Set<V> modSet = null;

        // n -> target
        modSet = new HashSet<V>(set);

        for (V tn : modSet) {
            this.tc.removeTuple(n, tn);
        }

        // source -> n
        set = tc.getTupleStarts(n);

        modSet = new HashSet<V>(set);

        for (V sn : modSet) {
            this.tc.removeTuple(sn, n);
        }
    }

    public DRedTcRelation<V> getTcRelation() {
        return this.tc;
    }

    public void setTcRelation(DRedTcRelation<V> tc) {
        this.tc = tc;
    }

    @Override
    public boolean isReachable(V source, V target) {
        return tc.containsTuple(source, target);
    }

    @Override
    public void attachObserver(ITcObserver<V> to) {
        this.observers.add(to);
    }

    @Override
    public void detachObserver(ITcObserver<V> to) {
        this.observers.remove(to);
    }

    @Override
    public Set<V> getAllReachableTargets(V source) {
        return tc.getTupleEnds(source);
    }

    @Override
    public Set<V> getAllReachableSources(V target) {
        return tc.getTupleStarts(target);
    }

    protected void notifyTcObservers(Set<Tuple<V>> tuples, int dir) {
        for (ITcObserver<V> o : observers) {
            for (Tuple<V> t : tuples) {
                if (!t.getSource().equals(t.getTarget())) {
                    if (dir == 1) {
                        o.tupleInserted(t.getSource(), t.getTarget());
                    }
                    if (dir == -1) {
                        o.tupleDeleted(t.getSource(), t.getTarget());
                    }
                }
            }
        }
    }

    @Override
    public void dispose() {
        tc = null;
        dtc = null;
    }

    @Override
    public IGraphPathFinder<V> getPathFinder() {
        return new DFSPathFinder<V>(graphDataSource, this);
    }
}
