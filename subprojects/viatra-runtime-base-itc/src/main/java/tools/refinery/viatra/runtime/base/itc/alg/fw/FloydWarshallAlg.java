/*******************************************************************************
 * Copyright (c) 2010-2012, Tamas Szabo, Istvan Rath and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package tools.refinery.viatra.runtime.base.itc.alg.fw;

import java.util.HashMap;
import java.util.Map;

import tools.refinery.viatra.runtime.base.itc.alg.dred.DRedTcRelation;
import tools.refinery.viatra.runtime.base.itc.igraph.IBiDirectionalGraphDataSource;
import tools.refinery.viatra.runtime.base.itc.igraph.IBiDirectionalWrapper;
import tools.refinery.viatra.runtime.base.itc.igraph.IGraphDataSource;
import tools.refinery.viatra.runtime.base.itc.igraph.IGraphObserver;
import tools.refinery.viatra.runtime.matchers.util.IMemoryView;

public class FloydWarshallAlg<V> implements IGraphObserver<V> {

    private DRedTcRelation<V> tc = null;
    private IBiDirectionalGraphDataSource<V> gds = null;

    public FloydWarshallAlg(IGraphDataSource<V> gds) {
        if (gds instanceof IBiDirectionalGraphDataSource<?>) {
            this.gds = (IBiDirectionalGraphDataSource<V>) gds;
        } else {
            this.gds = new IBiDirectionalWrapper<V>(gds);
        }

        this.tc = new DRedTcRelation<V>();
        gds.attachObserver(this);
        generateTc();
    }

    private void generateTc() {

        tc.clear();

        int n = gds.getAllNodes().size();
        Map<V, Integer> mapForw = new HashMap<V, Integer>();
        Map<Integer, V> mapBackw = new HashMap<Integer, V>();
        int[][] P = new int[n][n];

        int i, j, k;

        // initialize adjacent matrix
        for (i = 0; i < n; i++) {
            for (j = 0; j < n; j++) {
                P[i][j] = 0;
            }
        }

        i = 0;
        for (V node : gds.getAllNodes()) {
            mapForw.put(node, i);
            mapBackw.put(i, node);
            i++;
        }

        for (V source : gds.getAllNodes()) {
            IMemoryView<V> targets = gds.getTargetNodes(source);
            for (V target : targets.distinctValues()) {
                P[mapForw.get(source)][mapForw.get(target)] = 1;
            }
        }

        for (k = 0; k < n; k++) {
            for (i = 0; i < n; i++) {
                for (j = 0; j < n; j++) {
                    P[i][j] = P[i][j] | (P[i][k] & P[k][j]);
                }
            }
        }

        for (i = 0; i < n; i++) {
            for (j = 0; j < n; j++) {
                if (P[i][j] == 1 && i != j)
                    tc.addTuple(mapBackw.get(i), mapBackw.get(j));
            }
        }
    }

    @Override
    public void edgeInserted(V source, V target) {
        generateTc();
    }

    @Override
    public void edgeDeleted(V source, V target) {
        generateTc();
    }

    @Override
    public void nodeInserted(V n) {
        generateTc();
    }

    @Override
    public void nodeDeleted(V n) {
        generateTc();
    }

    public DRedTcRelation<V> getTcRelation() {
        return this.tc;
    }
}
