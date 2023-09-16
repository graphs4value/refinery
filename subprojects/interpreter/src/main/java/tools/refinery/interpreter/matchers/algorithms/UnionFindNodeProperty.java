/*******************************************************************************
 * Copyright (c) 2010-2012, Tamas Szabo, Istvan Rath and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package tools.refinery.interpreter.matchers.algorithms;

public class UnionFindNodeProperty<V> {

    public int rank;
    public V parent;

    public UnionFindNodeProperty() {
        this.rank = 0;
        this.parent = null;
    }

    public UnionFindNodeProperty(int rank, V parent) {
        super();
        this.rank = rank;
        this.parent = parent;
    }

    @Override
    public String toString() {
        return "[rank:" + rank + ", parent:" + parent.toString() + "]";
    }
}
