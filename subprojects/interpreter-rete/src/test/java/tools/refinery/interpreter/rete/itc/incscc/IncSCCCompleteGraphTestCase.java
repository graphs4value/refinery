/*******************************************************************************
 * Copyright (c) 2010-2012, Tamas Szabo, Istvan Rath and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.interpreter.rete.itc.incscc;

import org.junit.Test;
import tools.refinery.interpreter.rete.itc.alg.incscc.IncSCCAlg;
import tools.refinery.interpreter.rete.itc.alg.misc.dfs.DFSAlg;
import tools.refinery.interpreter.rete.itc.graphimpl.Graph;

import static org.junit.Assert.assertTrue;

public class IncSCCCompleteGraphTestCase {

    @Test
    public void testResult() {

        final int nodeCount = 10;
        Graph<Integer> graph = new Graph<Integer>();
        DFSAlg<Integer> dfsAlg = new DFSAlg<Integer>(graph);
        IncSCCAlg<Integer> incsccAlg = new IncSCCAlg<Integer>(graph);

        for (int i = 0; i < nodeCount; i++) {
            graph.insertNode(i);
        }

        for (int i = 0; i < nodeCount; i++) {
            for (int j = 0; j < nodeCount; j++) {
                if (i != j) {
                    graph.insertEdge(i, j);
                    assertTrue(incsccAlg.checkTcRelation(dfsAlg.getTcRelation()));
                }
            }
        }

        for (int i = 0; i < nodeCount; i++) {
            for (int j = 0; j < nodeCount; j++) {
                if (i != j) {
                    graph.deleteEdgeIfExists(i, j);
                    assertTrue(incsccAlg.checkTcRelation(dfsAlg.getTcRelation()));
                }
            }
        }
    }
}
