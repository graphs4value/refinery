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
 * This class provides a volatile tuple view with a given mask of a given tuple instance. If the masked tuple changes,
 * the view updates as well.
 *
 * @author Zoltan Ujhelyi
 * @since 1.7
 *
 */
public class VolatileMaskedTuple extends VolatileTuple {

    protected final TupleMask mask;
    protected ITuple source;

    public VolatileMaskedTuple(ITuple source, TupleMask mask) {
        this.source = source;
        this.mask = mask;
    }

    public VolatileMaskedTuple(TupleMask mask) {
        this(null, mask);
    }

    public void updateTuple(ITuple newSource) {
        source = newSource;
    }

    @Override
    public Object get(int index) {
        Preconditions.checkState(source != null, "Source tuple is not set.");
        return mask.getValue(source, index);
    }

    @Override
    public int getSize() {
        return mask.getSize();
    }

}
