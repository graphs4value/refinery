/*******************************************************************************
 * Copyright (c) 2010-2012, Tamas Szabo, Istvan Rath and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package tools.refinery.interpreter.rete.itc.alg.misc.scc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import tools.refinery.interpreter.rete.itc.igraph.IBiDirectionalGraphDataSource;
import tools.refinery.interpreter.rete.itc.igraph.IBiDirectionalWrapper;
import tools.refinery.interpreter.rete.itc.igraph.IGraphDataSource;
import tools.refinery.interpreter.rete.itc.igraph.IGraphObserver;

public class PKAlg<V> implements IGraphObserver<V> {

    /**
     * Maps the nodes to their indicies.
     */
    private Map<V, Integer> node2index;
    private Map<Integer, V> index2node;
    private Map<V, Boolean> node2mark;

    /**
     * Maps the index of a node to the index in the topsort.
     */
    private Map<Integer, Integer> index2topsort;
    private Map<Integer, Integer> topsort2index;

    /**
     * Index associated to the inserted nodes (incrementing with every insertion).
     */
    private int index;

    /**
     * Index within the topsort for the target node when edge insertion occurs.
     */
    private int lower_bound;

    /**
     * Index within the topsort for the source node when edge insertion occurs.
     */
    private int upper_bound;

    private List<V> RF;
    private List<V> RB;
    private IBiDirectionalGraphDataSource<V> gds;

    public PKAlg(IGraphDataSource<V> gds) {
        if (gds instanceof IBiDirectionalGraphDataSource<?>) {
            this.gds = (IBiDirectionalGraphDataSource<V>) gds;
        } else {
            this.gds = new IBiDirectionalWrapper<V>(gds);
        }

        node2mark = new HashMap<V, Boolean>();
        node2index = new HashMap<V, Integer>();
        index2node = new HashMap<Integer, V>();
        index2topsort = new HashMap<Integer, Integer>();
        topsort2index = new HashMap<Integer, Integer>();
        index = 0;

        gds.attachObserver(this);
    }

    @Override
    public void edgeInserted(V source, V target) {

        RF = new ArrayList<V>();
        RB = new ArrayList<V>();

        lower_bound = index2topsort.get(node2index.get(target));
        upper_bound = index2topsort.get(node2index.get(source));

        if (lower_bound < upper_bound) {
            dfsForward(target);
            dfsBackward(source);
            reorder();
        }
    }

    private List<Integer> getIndicies(List<V> list) {
        List<Integer> indicies = new ArrayList<Integer>();

        for (V n : list)
            indicies.add(index2topsort.get(node2index.get(n)));

        return indicies;
    }

    private void reorder() {

        Collections.reverse(RB);

        // azon csomopontok indexei amelyek sorrendje nem jo
        List<Integer> L = getIndicies(RF);
        L.addAll(getIndicies(RB));
        Collections.sort(L);

        for (int i = 0; i < RB.size(); i++) {
            index2topsort.put(node2index.get(RB.get(i)), L.get(i));
            topsort2index.put(L.get(i), node2index.get(RB.get(i)));
        }

        for (int i = 0; i < RF.size(); i++) {
            index2topsort.put(node2index.get(RF.get(i)), L.get(i + RB.size()));
            topsort2index.put(L.get(i + RB.size()), node2index.get(RF.get(i)));
        }
    }

    @SuppressWarnings("unused")
    private List<V> getTopSort() {
        List<V> topsort = new ArrayList<V>();

        for (int i : topsort2index.values()) {
            topsort.add(index2node.get(i));
        }

        return topsort;
    }

    private void dfsBackward(V node) {
        node2mark.put(node, true);
        RB.add(node);

        for (V sn : gds.getSourceNodes(node).distinctValues()) {
            int top_id = index2topsort.get(node2index.get(sn));

            if (!node2mark.get(sn) && lower_bound < top_id)
                dfsBackward(sn);
        }
    }

    private void dfsForward(V node) {
        node2mark.put(node, true);
        RF.add(node);

        for (V tn : gds.getTargetNodes(node).distinctValues()) {
            int top_id = index2topsort.get(node2index.get(tn));

            if (top_id == upper_bound)
                System.out.println("!!!Cycle detected!!!");
            else if (!node2mark.get(tn) && top_id < upper_bound)
                dfsForward(tn);
        }
    }

    @Override
    public void edgeDeleted(V source, V target) {
        // Edge deletion does not affect topsort
    }

    @Override
    public void nodeInserted(V n) {
        node2mark.put(n, false);
        node2index.put(n, index);
        index2node.put(index, n);
        index2topsort.put(index, index);
        topsort2index.put(index, index);
        index++;
    }

    @Override
    public void nodeDeleted(V n) {
        node2mark.remove(n);
        int node_id = node2index.remove(n);
        index2node.remove(node_id);
        int top_id = index2topsort.remove(node_id);
        topsort2index.remove(top_id);
    }
}
