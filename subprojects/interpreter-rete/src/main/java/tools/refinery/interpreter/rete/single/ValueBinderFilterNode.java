/*******************************************************************************
 * Copyright (c) 2004-2008 Gabor Bergmann and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package tools.refinery.interpreter.rete.single;

import tools.refinery.interpreter.rete.network.ReteContainer;
import tools.refinery.interpreter.matchers.tuple.Tuple;

/**
 * A filter node that keeps only those tuples that contain a certain value at a certain position.
 *
 * @author Bergmann Gabor
 *
 */
public class ValueBinderFilterNode extends FilterNode {

    int bindingIndex;
    Object bindingValue;

    /**
     * @param reteContainer
     * @param bindingIndex
     *            the position in the tuple that should be bound
     * @param bindingValue
     *            the value to which the tuple has to be bound
     */
    public ValueBinderFilterNode(ReteContainer reteContainer, int bindingIndex, Object bindingValue) {
        super(reteContainer);
        this.bindingIndex = bindingIndex;
        this.bindingValue = bindingValue;
    }

    @Override
    public boolean check(Tuple ps) {
        return bindingValue.equals(ps.get(bindingIndex));
    }

}
