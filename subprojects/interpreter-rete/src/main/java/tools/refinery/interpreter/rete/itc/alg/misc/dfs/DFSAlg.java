/*******************************************************************************
 * Copyright (c) 2010-2012, Tamas Szabo, Istvan Rath and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package tools.refinery.interpreter.rete.itc.alg.misc.dfs;

import tools.refinery.interpreter.rete.itc.alg.dred.DRedTcRelation;
import tools.refinery.interpreter.rete.itc.igraph.IGraphDataSource;
import tools.refinery.interpreter.rete.itc.igraph.IGraphObserver;

import java.util.HashMap;

public class DFSAlg<V> implements IGraphObserver<V> {

    private IGraphDataSource<V> gds;
    private DRedTcRelation<V> tc;
    private int[] visited;
    private HashMap<V, Integer> nodeMap;

    public DFSAlg(IGraphDataSource<V> gds) {
        this.gds = gds;
        this.tc = new DRedTcRelation<V>();
        gds.attachObserver(this);
        deriveTc();
    }

    private void deriveTc() {
        tc.clear();

        this.visited = new int[gds.getAllNodes().size()];
        nodeMap = new HashMap<V, Integer>();

        int j = 0;
        for (V n : gds.getAllNodes()) {
            nodeMap.put(n, j);
            j++;
        }

        for (V n : gds.getAllNodes()) {
            oneDFS(n, n);
            initVisitedArray();
        }
    }

    private void initVisitedArray() {
        for (int i = 0; i < visited.length; i++)
            visited[i] = 0;
    }

    private void oneDFS(V act, V source) {

        if (!act.equals(source)) {
            tc.addTuple(source, act);
        }

        visited[nodeMap.get(act)] = 1;

        for (V t : gds.getTargetNodes(act).distinctValues()) {
            if (visited[nodeMap.get(t)] == 0) {
                oneDFS(t, source);
            }
        }
    }

    public DRedTcRelation<V> getTcRelation() {
        return this.tc;
    }

    @Override
    public void edgeInserted(V source, V target) {
        deriveTc();
    }

    @Override
    public void edgeDeleted(V source, V target) {
        deriveTc();
    }

    @Override
    public void nodeInserted(V n) {
        deriveTc();
    }

    @Override
    public void nodeDeleted(V n) {
        deriveTc();
    }
}
