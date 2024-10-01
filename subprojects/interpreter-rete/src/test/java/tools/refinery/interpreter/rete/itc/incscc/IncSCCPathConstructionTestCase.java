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
import tools.refinery.interpreter.rete.itc.graphimpl.Graph;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class IncSCCPathConstructionTestCase {

    @Test
    public void pathConstructionTest1() {
        Graph<Integer> testGraph = new Graph<Integer>();
        IncSCCAlg<Integer> incscc = new IncSCCAlg<Integer>(testGraph);
        testGraph.insertNode(1);
        testGraph.insertNode(2);
        testGraph.insertNode(3);
        testGraph.insertNode(4);
        testGraph.insertNode(5);
        testGraph.insertNode(6);
        testGraph.insertNode(7);
        testGraph.insertNode(8);

        testGraph.insertEdge(1, 2);
        testGraph.insertEdge(2, 3);
        testGraph.insertEdge(3, 1);
        testGraph.insertEdge(3, 4);
        testGraph.insertEdge(4, 5);
        testGraph.insertEdge(3, 6);
        testGraph.insertEdge(6, 7);
        testGraph.insertEdge(7, 8);
        testGraph.insertEdge(8, 6);

        List<Integer> expectedPath = new ArrayList<Integer>();
        expectedPath.add(1);
        expectedPath.add(2);
        expectedPath.add(3);
        expectedPath.add(6);
        expectedPath.add(7);

        assertEquals(expectedPath, incscc.getReachabilityPath(1, 7));
    }

    @Test
    public void pathConstructionTest2() {
        Graph<Integer> testGraph = new Graph<Integer>();
        IncSCCAlg<Integer> incscc = new IncSCCAlg<Integer>(testGraph);
        testGraph.insertNode(1);
        testGraph.insertNode(2);
        testGraph.insertNode(3);
        testGraph.insertNode(4);
        testGraph.insertNode(5);
        testGraph.insertNode(6);
        testGraph.insertNode(7);
        testGraph.insertNode(8);

        testGraph.insertEdge(1, 2);
        testGraph.insertEdge(2, 3);
        testGraph.insertEdge(3, 4);
        testGraph.insertEdge(4, 5);
        testGraph.insertEdge(5, 2);
        testGraph.insertEdge(2, 6);
        testGraph.insertEdge(6, 7);
        testGraph.insertEdge(7, 2);

        assertNull(incscc.getReachabilityPath(1, 8));
    }

    @Test
    public void pathConstructionTest3() {
        Graph<Integer> testGraph = new Graph<Integer>();
        IncSCCAlg<Integer> incscc = new IncSCCAlg<Integer>(testGraph);
        testGraph.insertNode(1);
        testGraph.insertNode(2);
        testGraph.insertNode(3);
        testGraph.insertNode(4);
        testGraph.insertNode(5);
        testGraph.insertNode(6);
        testGraph.insertNode(7);
        testGraph.insertNode(8);

        testGraph.insertEdge(1, 2);
        testGraph.insertEdge(2, 3);
        testGraph.insertEdge(3, 4);
        testGraph.insertEdge(4, 5);
        testGraph.insertEdge(5, 2);
        testGraph.insertEdge(2, 6);
        testGraph.insertEdge(6, 7);
        testGraph.insertEdge(7, 2);
        testGraph.insertEdge(2, 8);

        List<Integer> expectedPath = new ArrayList<Integer>();
        expectedPath.add(1);
        expectedPath.add(2);
        expectedPath.add(8);

        assertEquals(expectedPath, incscc.getReachabilityPath(1, 8));
    }

    @Test
    public void pathConstructionTest4() {
        Graph<Integer> testGraph = new Graph<Integer>();
        IncSCCAlg<Integer> incscc = new IncSCCAlg<Integer>(testGraph);
        testGraph.insertNode(1);

        assertNull(incscc.getReachabilityPath(1, 1));

        testGraph.insertEdge(1, 1);

        List<Integer> expectedPath = new ArrayList<Integer>();
        expectedPath.add(1);
        expectedPath.add(1);

        assertEquals(expectedPath, incscc.getReachabilityPath(1, 1));
    }
}
