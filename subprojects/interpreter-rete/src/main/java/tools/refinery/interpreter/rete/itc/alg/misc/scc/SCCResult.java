/*******************************************************************************
 * Copyright (c) 2010-2012, Tamas Szabo, Istvan Rath and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package tools.refinery.interpreter.rete.itc.alg.misc.scc;

import java.util.Map.Entry;
import java.util.Set;

import tools.refinery.interpreter.rete.itc.igraph.IGraphDataSource;

public class SCCResult<V> {

    private Set<Set<V>> sccs;
    private IGraphDataSource<V> gds;

    public SCCResult(Set<Set<V>> sccs, IGraphDataSource<V> gds) {
        this.sccs = sccs;
        this.gds = gds;
    }

    public Set<Set<V>> getSccs() {
        return sccs;
    }

    public int getSCCCount() {
        return sccs.size();
    }

    public double getAverageNodeCount() {
        double a = 0;

        for (Set<V> s : sccs) {
            a += s.size();
        }

        return a / sccs.size();
    }

    public double getAverageEdgeCount() {
        long edgeSum = 0;

        for (Set<V> scc : sccs) {
            for (V source : scc) {
                for (Entry<V, Integer> entry : gds.getTargetNodes(source).entriesWithMultiplicities()) {
                    if (scc.contains(entry.getKey())) {
                        edgeSum += entry.getValue();
                    }
                }
            }
        }

        return (double) edgeSum / (double) sccs.size();
    }

    public int getBiggestSCCSize() {
        int max = 0;

        for (Set<V> scc : sccs) {
            if (scc.size() > max)
                max = scc.size();
        }

        return max;
    }

    public long getSumOfSquares() {
        long sum = 0;

        for (Set<V> scc : sccs) {
            sum += scc.size() * scc.size();
        }

        return sum;
    }
}
