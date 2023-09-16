/*******************************************************************************
 * Copyright (c) 2010-2012, Tamas Szabo, Istvan Rath and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package tools.refinery.interpreter.rete.itc.alg.misc.scc;

public class SCCProperty {
    private int index;
    private int lowlink;

    public SCCProperty(int index, int lowlink) {
        super();
        this.index = index;
        this.lowlink = lowlink;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public int getLowlink() {
        return lowlink;
    }

    public void setLowlink(int lowlink) {
        this.lowlink = lowlink;
    }
}
