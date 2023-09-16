/*******************************************************************************
 * Copyright (c) 2010-2017, Gabor Bergmann, IncQueryLabs Ltd.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.interpreter.matchers.tuple;

import java.util.List;

/**
 * @author Gabor Bergmann
 * @since 1.7
 */
public final class TupleMaskIdentity extends TupleMask {

    TupleMaskIdentity(int sourceWidth) {
        this(constructLinearSequence(sourceWidth), sourceWidth);
    }
    TupleMaskIdentity(int[] indices, int sourceWidth) {
        super(indices, sourceWidth, indices, true);
    }

    @Override
    public <T> List<T> transform(List<T> original) {
        return original;
    }

    @Override
    public Tuple transform(ITuple original) {
        return original.toImmutable();
    }

    @Override
    public TupleMask transform(TupleMask mask) {
        return mask;
    }

    @Override
    public Tuple revertFrom(ITuple masked) {
        return masked.toImmutable();
    }

    @Override
    public boolean isIdentity() {
        return true;
    }

}
