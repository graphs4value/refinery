/*******************************************************************************
 * Copyright (c) 2010-2012, Tamas Szabo, Istvan Rath and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.interpreter.rete.itc.alg.incscc;

import tools.refinery.interpreter.rete.itc.igraph.ITcObserver;
import tools.refinery.interpreter.matchers.util.Direction;

/**
 * @author Tamas Szabo
 *
 */
public class CountingListener<V> implements ITcObserver<V> {

    private IncSCCAlg<V> alg;

    public CountingListener(IncSCCAlg<V> alg) {
        this.alg = alg;
    }

    @Override
    public void tupleInserted(V source, V target) {
        alg.notifyTcObservers(alg.sccs.getPartition(source), alg.sccs.getPartition(target), Direction.INSERT);
    }

    @Override
    public void tupleDeleted(V source, V target) {
        alg.notifyTcObservers(alg.sccs.getPartition(source), alg.sccs.getPartition(target), Direction.DELETE);
    }

}
