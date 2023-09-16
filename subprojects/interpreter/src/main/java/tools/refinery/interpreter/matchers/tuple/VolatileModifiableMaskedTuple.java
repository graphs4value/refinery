/*******************************************************************************
 * Copyright (c) 2010-2017, Zoltan Ujhelyi, IncQuery Labs Ltd.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.interpreter.matchers.tuple;

import tools.refinery.interpreter.matchers.util.Preconditions;

/**
 * A masked tuple implementation that allows modifying the backing tuple.
 * @author Zoltan Ujhelyi
 * @since 1.7
 *
 */
public class VolatileModifiableMaskedTuple extends VolatileMaskedTuple implements IModifiableTuple {

    private IModifiableTuple modifiableTuple;

    public VolatileModifiableMaskedTuple(IModifiableTuple source, TupleMask mask) {
        super(source, mask);
        modifiableTuple = source;
    }

    public VolatileModifiableMaskedTuple(TupleMask mask) {
        this(null, mask);
    }

    @Override
    public void updateTuple(ITuple newSource) {
        Preconditions.checkArgument(newSource instanceof IModifiableTuple, "Provided tuple does not support updates");
        this.updateTuple((IModifiableTuple)newSource);
    }

    public void updateTuple(IModifiableTuple newSource) {
        super.updateTuple(newSource);
        modifiableTuple = newSource;
    }

    @Override
    public void set(int index, Object value) {
        mask.set(modifiableTuple, index, value);
    }
}
