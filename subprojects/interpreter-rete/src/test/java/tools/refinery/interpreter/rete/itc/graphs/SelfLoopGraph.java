/*******************************************************************************
 * Copyright (c) 2010-2012, Abel Hegedus, Istvan Rath and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.interpreter.rete.itc.graphs;

/**
 * @author Abel Hegedus
 *
 */
public class SelfLoopGraph extends TestGraph<Integer> {

    public SelfLoopGraph() {
        super(null);
    }

    @Override
    public void modify() {
        Integer n1 = Integer.valueOf(1);
        this.insertNode(n1);
        this.insertEdge(n1, n1);
        this.deleteEdgeIfExists(n1, n1);
    }

}
