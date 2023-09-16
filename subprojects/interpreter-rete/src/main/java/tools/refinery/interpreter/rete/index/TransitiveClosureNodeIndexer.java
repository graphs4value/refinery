/*******************************************************************************
 * Copyright (c) 2010-2012, Tamas Szabo, Gabor Bergmann, Istvan Rath and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.interpreter.rete.index;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Set;

import tools.refinery.interpreter.rete.itc.alg.misc.Tuple;
import tools.refinery.interpreter.rete.network.Receiver;
import tools.refinery.interpreter.rete.single.TransitiveClosureNode;
import tools.refinery.interpreter.rete.itc.alg.incscc.IncSCCAlg;
import tools.refinery.interpreter.matchers.tuple.MaskedTuple;
import tools.refinery.interpreter.matchers.tuple.TupleMask;
import tools.refinery.interpreter.matchers.tuple.Tuples;
import tools.refinery.interpreter.matchers.util.CollectionsFactory;
import tools.refinery.interpreter.matchers.util.Direction;

// UNFINISHED, not used yet
public class TransitiveClosureNodeIndexer extends StandardIndexer implements IterableIndexer {
    private TransitiveClosureNode tcNode;
    private IncSCCAlg<Object> tcAlg;
    private Collection<tools.refinery.interpreter.matchers.tuple.Tuple> emptySet;

    public TransitiveClosureNodeIndexer(TupleMask mask, IncSCCAlg<Object> tcAlg, TransitiveClosureNode tcNode) {
        super(tcNode.getContainer(), mask);
        this.tcAlg = tcAlg;
        this.tcNode = tcNode;
        this.emptySet = Collections.emptySet();
        this.parent = tcNode;
    }

    @Override
    public Collection<tools.refinery.interpreter.matchers.tuple.Tuple> get(tools.refinery.interpreter.matchers.tuple.Tuple signature) {
        if (signature.getSize() == mask.sourceWidth) {
            if (mask.indices.length == 0) {
                // mask ()/2
                return getSignatures();
            } else if (mask.indices.length == 1) {
                Set<tools.refinery.interpreter.matchers.tuple.Tuple> retSet = CollectionsFactory.createSet();

                // mask (0)/2
                if (mask.indices[0] == 0) {
                    Object source = signature.get(0);
                    for (Object target : tcAlg.getAllReachableTargets(source)) {
                        retSet.add(Tuples.staticArityFlatTupleOf(source, target));
                    }
                    return retSet;
                }
                // mask (1)/2
                if (mask.indices[0] == 1) {
                    Object target = signature.get(1);
                    for (Object source : tcAlg.getAllReachableSources(target)) {
                        retSet.add(Tuples.staticArityFlatTupleOf(source, target));
                    }
                    return retSet;
                }
            } else {
                // mask (0,1)/2
                if (mask.indices[0] == 0 && mask.indices[1] == 1) {
                    Object source = signature.get(0);
                    Object target = signature.get(1);
                    tools.refinery.interpreter.matchers.tuple.Tuple singleton = Tuples.staticArityFlatTupleOf(source, target);
                    return (tcAlg.isReachable(source, target) ? Collections.singleton(singleton) : emptySet);
                }
                // mask (1,0)/2
                if (mask.indices[0] == 1 && mask.indices[1] == 0) {
                    Object source = signature.get(1);
                    Object target = signature.get(0);
                    tools.refinery.interpreter.matchers.tuple.Tuple singleton = Tuples.staticArityFlatTupleOf(source, target);
                    return (tcAlg.isReachable(source, target) ? Collections.singleton(singleton) : emptySet);
                }
            }
        }
        return null;
    }

    @Override
    public int getBucketCount() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Collection<tools.refinery.interpreter.matchers.tuple.Tuple> getSignatures() {
        return asTupleCollection(tcAlg.getTcRelation());
    }

    @Override
    public Iterator<tools.refinery.interpreter.matchers.tuple.Tuple> iterator() {
        return asTupleCollection(tcAlg.getTcRelation()).iterator();
    }

    private Collection<tools.refinery.interpreter.matchers.tuple.Tuple> asTupleCollection(
            Collection<Tuple<Object>> tuples) {
        Set<tools.refinery.interpreter.matchers.tuple.Tuple> retSet = CollectionsFactory.createSet();
        for (Tuple<Object> tuple : tuples) {
            retSet.add(Tuples.staticArityFlatTupleOf(tuple.getSource(), tuple.getTarget()));
        }
        return retSet;
    }

    /**
     * @since 2.4
     */
    public void propagate(Direction direction, tools.refinery.interpreter.matchers.tuple.Tuple updateElement, boolean change) {
        propagate(direction, updateElement, new MaskedTuple(updateElement, mask), change, null);
    }

    @Override
    public Receiver getActiveNode() {
        return tcNode;
    }

}
