/*******************************************************************************
 * Copyright (c) 2010-2012, Tamas Szabo, Istvan Rath and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.interpreter.rete.itc.graphs;

import tools.refinery.interpreter.rete.itc.alg.misc.Tuple;
import tools.refinery.interpreter.rete.itc.misc.TestObserver;

public class Graph7 extends TestGraph<Integer> {

    public Graph7() {
        super(new TestObserver<Integer>());
    }

    public void modify() {
        Integer n1 = Integer.valueOf(1);
        Integer n2 = Integer.valueOf(2);
        Integer n3 = Integer.valueOf(3);

        this.insertNode(n1);
        this.insertNode(n2);
        this.insertNode(n3);

        this.observer.clearTuples();
        this.observer.addInsertedTuple(new Tuple<Integer>(n1, n2));
        this.insertEdge(n1, n2);

        this.observer.clearTuples();
        this.observer.addInsertedTuple(new Tuple<Integer>(n2, n1));
        this.observer.addInsertedTuple(new Tuple<Integer>(n1, n1));
        this.observer.addInsertedTuple(new Tuple<Integer>(n2, n2));
        this.insertEdge(n2, n1);

        this.observer.clearTuples();
        this.observer.addInsertedTuple(new Tuple<Integer>(n3, n1));
        this.observer.addInsertedTuple(new Tuple<Integer>(n3, n2));
        this.insertEdge(n3, n2);

        this.observer.clearTuples();
        this.observer.addInsertedTuple(new Tuple<Integer>(n1, n3));
        this.observer.addInsertedTuple(new Tuple<Integer>(n2, n3));
        this.observer.addInsertedTuple(new Tuple<Integer>(n3, n3));
        this.insertEdge(n2, n3);

        this.observer.clearTuples();
        this.observer.addDeletedTuple(new Tuple<Integer>(n1, n1));
        this.observer.addDeletedTuple(new Tuple<Integer>(n1, n2));
        this.observer.addDeletedTuple(new Tuple<Integer>(n1, n3));
        this.deleteEdgeIfExists(n1, n2);

    }
}
