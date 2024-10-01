/*******************************************************************************
 * Copyright (c) 2010-2012, Tamas Szabo, Istvan Rath and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.interpreter.rete.itc.relations;

import tools.refinery.interpreter.rete.itc.alg.dred.DRedTcRelation;

public class DRedTcRelation2 extends DRedTcRelation<Integer> {

    public DRedTcRelation2() {
        this.addTuple(1, 2);
        this.addTuple(1, 6);

        this.addTuple(2, 6);

        this.addTuple(3, 4);
        this.addTuple(3, 5);

        this.addTuple(4, 5);
        this.addTuple(4, 3);

        this.addTuple(5, 3);
        this.addTuple(5, 4);
    }
}
