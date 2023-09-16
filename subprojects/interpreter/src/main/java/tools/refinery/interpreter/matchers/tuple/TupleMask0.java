/*******************************************************************************
 * Copyright (c) 2010-2017, Gabor Bergmann, IncQueryLabs Ltd.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.interpreter.matchers.tuple;

import java.util.Collections;
import java.util.List;

/**
 * @author Gabor Bergmann
 * @since 1.7
 */
public final class TupleMask0 extends TupleMask {

    private final static int[] EMPTY_ARRAY = {};

    /**
     * PRE: indices.length == 0
     */
    TupleMask0(int sourceWidth) {
        super(EMPTY_ARRAY, sourceWidth, EMPTY_ARRAY, true);
    }

    @Override
    public <T> List<T> transform(List<T> original) {
        return Collections.emptyList();
    }

    @Override
    public Tuple transform(ITuple original) {
        return Tuples.staticArityFlatTupleOf();
    }

    @Override
    public TupleMask transform(TupleMask mask) {
        return new TupleMask0(mask.sourceWidth);
    }

    @Override
    public Tuple combine(Tuple unmasked, Tuple masked, boolean useInheritance, boolean asComplementer) {
        if (asComplementer)
            return unmasked;
        else
            return super.combine(unmasked, masked, useInheritance, asComplementer);
    }

    @Override
    public boolean isIdentity() {
        return 0 == sourceWidth;
    }
}
