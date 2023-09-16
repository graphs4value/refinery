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

public class EqualityFilterNode extends FilterNode {

    int[] indices;
    int first;

    /**
     * @param reteContainer
     * @param indices
     *            indices of the Tuple that should hold equal values
     */
    public EqualityFilterNode(ReteContainer reteContainer, int[] indices) {
        super(reteContainer);
        this.indices = indices;
        first = indices[0];
    }

    @Override
    public boolean check(Tuple ps) {
        Object firstElement = ps.get(first);
        for (int i = 1 /* first is omitted */; i < indices.length; i++) {
            if (!ps.get(indices[i]).equals(firstElement))
                return false;
        }
        return true;
    }

}
