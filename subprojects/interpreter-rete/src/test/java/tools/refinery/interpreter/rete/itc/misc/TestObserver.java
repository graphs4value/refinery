/*******************************************************************************
 * Copyright (c) 2010-2012, Tamas Szabo, Istvan Rath and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.interpreter.rete.itc.misc;

import tools.refinery.interpreter.rete.itc.alg.misc.Tuple;
import tools.refinery.interpreter.rete.itc.igraph.ITcObserver;

import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertTrue;

/**
 * The {@link TestObserver} class can be used to assert the notifications of a
 * transitive closure algorithm. Before each edge deletion/insertion set the
 * expected tuples of notifications and the observer assert these tuples.
 * Don't forget to erase the contents of the observer before the next
 * edge manipulation.
 *
 * @author Tamas Szabo
 *
 * @param <V> the type parameter of the tuples
 */
public class TestObserver<V> implements ITcObserver<V> {

    private Set<Tuple<V>> deletedTuples;
    private Set<Tuple<V>> insertedTuples;

    public TestObserver() {
        this.deletedTuples = new HashSet<Tuple<V>>();
        this.insertedTuples = new HashSet<Tuple<V>>();
    }

    public void addDeletedTuple(Tuple<V> tuple) {
        this.deletedTuples.add(tuple);
    }

    public void addInsertedTuple(Tuple<V> tuple) {
        this.insertedTuples.add(tuple);
    }

    public void clearTuples() {
        this.deletedTuples.clear();
        this.insertedTuples.clear();
    }

    @Override
    public void tupleInserted(V source, V target) {
        assertTrue(this.insertedTuples.contains(new Tuple<V>(source, target)));
    }

    @Override
    public void tupleDeleted(V source, V target) {
        assertTrue(this.deletedTuples.contains(new Tuple<V>(source, target)));
    }

}
