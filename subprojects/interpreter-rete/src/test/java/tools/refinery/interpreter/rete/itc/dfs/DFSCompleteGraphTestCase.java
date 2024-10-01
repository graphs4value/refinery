/*******************************************************************************
 * Copyright (c) 2010-2012, Tamas Szabo, Istvan Rath and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.interpreter.rete.itc.dfs;

import org.junit.Test;
import tools.refinery.interpreter.rete.itc.alg.fw.FloydWarshallAlg;
import tools.refinery.interpreter.rete.itc.alg.misc.dfs.DFSAlg;
import tools.refinery.interpreter.rete.itc.graphimpl.Graph;

import static org.junit.Assert.assertEquals;

public class DFSCompleteGraphTestCase {

    @Test
    public void testResult() {

        FloydWarshallAlg<Integer> fwa = null;
        int nodeCount = 10;
        Graph<Integer> g = new Graph<Integer>();
        DFSAlg<Integer> da = new DFSAlg<Integer>(g);

        for (int i = 0; i < nodeCount; i++) {
            g.insertNode(i);
        }

        // inserting edges
        for (int i = 0; i < nodeCount; i++) {
            for (int j = 0; j < nodeCount; j++) {
                if (i != j) {
                    g.insertEdge(i, j);

                    fwa = new FloydWarshallAlg<Integer>(g);

                    assertEquals(da.getTcRelation(), fwa.getTcRelation());
                }
            }
        }

        for (int i = 0; i < nodeCount; i++) {
            for (int j = 0; j < nodeCount; j++) {
                if (i != j) {
                    g.deleteEdgeIfExists(i, j);

                    fwa = new FloydWarshallAlg<Integer>(g);

                    assertEquals(da.getTcRelation(), fwa.getTcRelation());
                }
            }
        }
    }
}

