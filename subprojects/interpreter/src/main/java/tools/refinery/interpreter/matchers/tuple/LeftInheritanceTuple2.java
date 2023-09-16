/*******************************************************************************
 * Copyright (c) 2010-2017, Gabor Bergmann, IncQueryLabs Ltd.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.interpreter.matchers.tuple;

import java.util.Objects;

/**
 * @author Gabor Bergmann
 * @since 1.7
 */
public final class LeftInheritanceTuple2 extends BaseLeftInheritanceTuple {
    private final Object localElement0;
    private final Object localElement1;

    protected LeftInheritanceTuple2(Tuple ancestor, Object localElement0, Object localElement1) {
        super(ancestor);
        this.localElement0 = localElement0;
        this.localElement1 = localElement1;
        calcHash();
    }

    @Override
    public int getLocalSize() {
        return 2;
    }

    @Override
    public int getSize() {
        return inheritedIndex + 2;
    }

    @Override
    public Object get(int index) {
        int local = index - inheritedIndex;
        if (local < 0)
            return ancestor.get(index);
        else if (local == 0) return localElement0;
        else if (local == 1) return localElement1;
        else throw raiseIndexingError(index);
    }

    /**
     * Optimized hash calculation
     */
    @Override
    void calcHash() {
        final int PRIME = 31;
        cachedHash = ancestor.hashCode();
        cachedHash = PRIME * cachedHash;
        if (localElement0 != null) cachedHash += localElement0.hashCode();
        cachedHash = PRIME * cachedHash;
        if (localElement1 != null) cachedHash += localElement1.hashCode();
    }

    @Override
    protected boolean localEquals(BaseLeftInheritanceTuple other) {
        if (other instanceof LeftInheritanceTuple2) {
            LeftInheritanceTuple2 lit = (LeftInheritanceTuple2)other;
            return Objects.equals(this.localElement0, lit.localElement0) &&
                    Objects.equals(this.localElement1, lit.localElement1);
        } else {
            return (2 == other.getLocalSize()) &&
                    Objects.equals(localElement0, other.get(inheritedIndex)) &&
                    Objects.equals(localElement1, other.get(inheritedIndex + 1));
        }
    }
}
