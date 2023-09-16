/*******************************************************************************
 * Copyright (c) 2010-2017 Zoltan Ujhelyi, IncQuery Labs
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package tools.refinery.interpreter.matchers.tuple;

/**
 * Mutable tuple without explicit modification commands. In practical terms, the values stored in a volatile tuple can
 * be changed without any notification.
 *
 * @author Zoltan Ujhelyi
 * @since 1.7
 *
 */
public abstract class VolatileTuple extends AbstractTuple {

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (!(obj instanceof ITuple))
            return false;
        final ITuple other = (ITuple) obj;
        return internalEquals(other);
    }

    @Override
    public int hashCode() {
        return doCalcHash();
    }

    /**
     * Creates an immutable tuple from the values stored in the tuple. The created tuple will not be updated when the
     * current tuple changes.
     */
    @Override
    public Tuple toImmutable() {
        return Tuples.flatTupleOf(getElements());
    }
}
