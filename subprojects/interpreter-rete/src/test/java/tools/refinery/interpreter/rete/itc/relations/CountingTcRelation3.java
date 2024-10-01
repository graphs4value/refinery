/*******************************************************************************
 * Copyright (c) 2010-2012, Tamas Szabo, Istvan Rath and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.interpreter.rete.itc.relations;

import tools.refinery.interpreter.rete.itc.alg.counting.CountingTcRelation;

public class CountingTcRelation3 extends CountingTcRelation<Integer> {

    public CountingTcRelation3() {
        super(true);
        this.addTuple(1, 2, 1);
        this.addTuple(1, 3, 1);
        this.addTuple(1, 4, 1);

        this.addTuple(2, 3, 1);
        this.addTuple(2, 4, 1);

        this.addTuple(4, 3, 1);

        this.addTuple(5, 6, 1);
    }
}
