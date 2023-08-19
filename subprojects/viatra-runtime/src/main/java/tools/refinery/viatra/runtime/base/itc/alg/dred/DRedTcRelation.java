/*******************************************************************************
 * Copyright (c) 2010-2012, Tamas Szabo, Istvan Rath and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package tools.refinery.viatra.runtime.base.itc.alg.dred;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import tools.refinery.viatra.runtime.base.itc.alg.misc.ITcRelation;

public class DRedTcRelation<V> implements ITcRelation<V> {

    // tc(a,b) means that b is transitively reachable from a
    private Map<V, Set<V>> tuplesForward;

    // data structure to efficiently get those nodes from which a given node is reachable
    // symmetric to tuplesForward
    private Map<V, Set<V>> tuplesBackward;

    public DRedTcRelation() {
        this.tuplesForward = new HashMap<V, Set<V>>();
        this.tuplesBackward = new HashMap<V, Set<V>>();
    }

    public void clear() {
        this.tuplesForward.clear();
        this.tuplesBackward.clear();
    }

    public boolean isEmpty() {
        return tuplesForward.isEmpty();
    }

    public void removeTuple(V source, V target) {

        // removing tuple from 'forward' tc relation
        Set<V> sSet = tuplesForward.get(source);
        if (sSet != null) {
            sSet.remove(target);
            if (sSet.size() == 0)
                tuplesForward.remove(source);
        }

        // removing tuple from 'backward' tc relation
        Set<V> tSet = tuplesBackward.get(target);
        if (tSet != null) {
            tSet.remove(source);
            if (tSet.size() == 0)
                tuplesBackward.remove(target);
        }
    }

    /**
     * Returns true if the tc relation did not contain previously such a tuple that is defined by (source,target), false
     * otherwise.
     * 
     * @param source
     *            the source of the tuple
     * @param target
     *            the target of the tuple
     * @return true if the relation did not contain previously the tuple
     */
    public boolean addTuple(V source, V target) {

        // symmetric modification, it is sufficient to check the return value in one collection
        // adding tuple to 'forward' tc relation
        Set<V> sSet = tuplesForward.get(source);
        if (sSet == null) {
            Set<V> newSet = new HashSet<V>();
            newSet.add(target);
            tuplesForward.put(source, newSet);
        } else {
            sSet.add(target);
        }

        // adding tuple to 'backward' tc relation
        Set<V> tSet = tuplesBackward.get(target);
        if (tSet == null) {
            Set<V> newSet = new HashSet<V>();
            newSet.add(source);
            tuplesBackward.put(target, newSet);
            return true;
        } else {
            boolean ret = tSet.add(source);
            return ret;
        }

    }

    /**
     * Union operation of two tc realtions.
     * 
     * @param rA
     *            the other tc relation
     */
    public void union(DRedTcRelation<V> rA) {
        for (V source : rA.tuplesForward.keySet()) {
            for (V target : rA.tuplesForward.get(source)) {
                this.addTuple(source, target);
            }
        }
    }

    /**
     * Computes the difference of this tc relation and the given rA parameter.
     * 
     * @param rA
     *            the subtrahend relation
     */
    public void difference(DRedTcRelation<V> rA) {
        for (V source : rA.tuplesForward.keySet()) {
            for (V target : rA.tuplesForward.get(source)) {
                this.removeTuple(source, target);
            }
        }
    }

    @Override
    public Set<V> getTupleEnds(V source) {
        Set<V> t = tuplesForward.get(source);
        return (t == null) ? new HashSet<V>() : new HashSet<V>(t);
    }

    /**
     * Returns the set of nodes from which the target node is reachable.
     * 
     * @param target
     *            the target node
     * @return the set of source nodes
     */
    public Set<V> getTupleStarts(V target) {
        Set<V> t = tuplesBackward.get(target);
        return (t == null) ? new HashSet<V>() : new HashSet<V>(t);
    }

    @Override
    public Set<V> getTupleStarts() {
        Set<V> t = tuplesForward.keySet();
        return new HashSet<V>(t);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("TcRelation = ");

        for (Entry<V, Set<V>> entry : this.tuplesForward.entrySet()) {
            V source = entry.getKey();
            for (V target : entry.getValue()) {
                sb.append("(" + source + "," + target + ") ");
            }
        }
        return sb.toString();
    }

    /**
     * Returns true if a (source, target) node is present in the transitive closure relation, false otherwise.
     * 
     * @param source
     *            the source node
     * @param target
     *            the target node
     * @return true if tuple is present, false otherwise
     */
    public boolean containsTuple(V source, V target) {
        if (tuplesForward.containsKey(source)) {
            if (tuplesForward.get(source).contains(target))
                return true;
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if ((obj == null) || (obj.getClass() != this.getClass())) {
            return false;
        }

        DRedTcRelation<V> aTR = (DRedTcRelation<V>) obj;

        for (Entry<V, Set<V>> entry : aTR.tuplesForward.entrySet()) {
            V source = entry.getKey();
            for (V target : entry.getValue()) {
                if (!this.containsTuple(source, target))
                    return false;
            }
        }

        for (Entry<V, Set<V>> entry : this.tuplesForward.entrySet()) {
            V source = entry.getKey();
            for (V target : entry.getValue()) {
                if (!aTR.containsTuple(source, target))
                    return false;
            }
        }

        return true;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 31 * hash + tuplesForward.hashCode();
        hash = 31 * hash + tuplesBackward.hashCode();
        return hash;
    }

    public Map<V, Set<V>> getTuplesForward() {
        return tuplesForward;
    }
}
