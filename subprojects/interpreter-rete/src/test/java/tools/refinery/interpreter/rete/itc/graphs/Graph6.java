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

public class Graph6 extends TestGraph<Integer> {

    public Graph6() {
        super(new TestObserver<Integer>());
    }

    public void modify() {
        Integer n1 = Integer.valueOf(1);
        Integer n2 = Integer.valueOf(2);
        Integer n3 = Integer.valueOf(3);
        Integer n4 = Integer.valueOf(4);
        Integer n5 = Integer.valueOf(5);
        Integer n6 = Integer.valueOf(6);
        Integer n7 = Integer.valueOf(7);

        this.insertNode(n1);
        this.insertNode(n2);
        this.insertNode(n3);
        this.insertNode(n4);
        this.insertNode(n5);
        this.insertNode(n6);
        this.insertNode(n7);

        this.observer.clearTuples();
        this.observer.addInsertedTuple(new Tuple<Integer>(n1, n2));
        this.insertEdge(n1, n2);

        this.observer.clearTuples();
        this.observer.addInsertedTuple(new Tuple<Integer>(n4, n5));
        this.insertEdge(n4, n5);

        this.observer.clearTuples();
        this.observer.addInsertedTuple(new Tuple<Integer>(n2, n4));
        this.observer.addInsertedTuple(new Tuple<Integer>(n2, n5));
        this.observer.addInsertedTuple(new Tuple<Integer>(n1, n4));
        this.observer.addInsertedTuple(new Tuple<Integer>(n1, n5));
        this.insertEdge(n2, n4);

        this.observer.clearTuples();
        this.observer.addInsertedTuple(new Tuple<Integer>(n3, n5));
        this.insertEdge(n3, n5);

        this.observer.clearTuples();
        this.observer.addInsertedTuple(new Tuple<Integer>(n5, n6));
        this.observer.addInsertedTuple(new Tuple<Integer>(n4, n6));
        this.observer.addInsertedTuple(new Tuple<Integer>(n3, n6));
        this.observer.addInsertedTuple(new Tuple<Integer>(n2, n6));
        this.observer.addInsertedTuple(new Tuple<Integer>(n1, n6));
        this.insertEdge(n5, n6);

        this.observer.clearTuples();
        this.observer.addInsertedTuple(new Tuple<Integer>(n6, n7));
        this.observer.addInsertedTuple(new Tuple<Integer>(n5, n7));
        this.observer.addInsertedTuple(new Tuple<Integer>(n4, n7));
        this.observer.addInsertedTuple(new Tuple<Integer>(n3, n7));
        this.observer.addInsertedTuple(new Tuple<Integer>(n2, n7));
        this.observer.addInsertedTuple(new Tuple<Integer>(n1, n7));
        this.insertEdge(n6, n7);

        this.observer.clearTuples();
        this.observer.addInsertedTuple(new Tuple<Integer>(n2, n3));
        this.observer.addInsertedTuple(new Tuple<Integer>(n1, n3));
        this.insertEdge(n2, n3);

        this.observer.clearTuples();
        this.observer.addInsertedTuple(new Tuple<Integer>(n2, n3));
        this.observer.addInsertedTuple(new Tuple<Integer>(n1, n3));
        this.observer.addInsertedTuple(new Tuple<Integer>(n3, n5));
        this.observer.addInsertedTuple(new Tuple<Integer>(n3, n6));
        this.observer.addInsertedTuple(new Tuple<Integer>(n3, n7));
        this.insertEdge(n3, n5);

        this.observer.clearTuples();
        this.observer.addInsertedTuple(new Tuple<Integer>(n1, n5));
        this.observer.addInsertedTuple(new Tuple<Integer>(n1, n6));
        this.observer.addInsertedTuple(new Tuple<Integer>(n1, n7));
        this.observer.addInsertedTuple(new Tuple<Integer>(n2, n5));
        this.observer.addInsertedTuple(new Tuple<Integer>(n2, n6));
        this.observer.addInsertedTuple(new Tuple<Integer>(n2, n7));
        this.observer.addInsertedTuple(new Tuple<Integer>(n4, n5));
        this.observer.addInsertedTuple(new Tuple<Integer>(n4, n6));
        this.observer.addInsertedTuple(new Tuple<Integer>(n4, n7));
        this.insertEdge(n4, n5);
    }
}
