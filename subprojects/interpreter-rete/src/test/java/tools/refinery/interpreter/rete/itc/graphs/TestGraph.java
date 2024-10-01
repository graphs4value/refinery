/*******************************************************************************
 * Copyright (c) 2010-2012, Tamas Szabo, Istvan Rath and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.interpreter.rete.itc.graphs;

import tools.refinery.interpreter.rete.itc.graphimpl.Graph;
import tools.refinery.interpreter.rete.itc.misc.TestObserver;

public abstract class TestGraph<T> extends Graph<T> {

    protected
	TestObserver<Integer> observer;

    public TestGraph(TestObserver<Integer> observer) {
        this.observer = observer;
    }

    public abstract void modify();

    public TestObserver<Integer> getObserver() {
        return observer;
    }

}
