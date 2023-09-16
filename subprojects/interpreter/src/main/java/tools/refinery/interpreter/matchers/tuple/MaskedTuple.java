/*******************************************************************************
 * Copyright (c) 2004-2009 Gabor Bergmann and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package tools.refinery.interpreter.matchers.tuple;

/**
 * A tuple that transparently provides a masked (transformed) view of another tuple.
 *
 * @author Gabor Bergmann
 * @since 2.0
 *
 */
public class MaskedTuple extends Tuple {

    Tuple wrapped;
    TupleMask mask;

    public MaskedTuple(Tuple wrapped, TupleMask mask) {
        super();
        // if (wrapped instanceof MaskedTuple) {
        // MaskedTuple parent = (MaskedTuple)wrapped;
        // this.wrapped = parent.wrapped;
        // this.mask = mask.transform(parent.mask);
        // }
        // else
        //{
            this.wrapped = wrapped;
            this.mask = mask;
        //}
    }

    @Override
    public Object get(int index) {
        return wrapped.get(mask.indices[index]);
    }

    @Override
    public int getSize() {
        return mask.indices.length;
    }

}
