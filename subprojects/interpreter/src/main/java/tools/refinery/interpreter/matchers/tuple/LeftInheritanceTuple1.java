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
public final class LeftInheritanceTuple1 extends BaseLeftInheritanceTuple {
    /**
     * A single substituted value after inheritedIndex.
     */
    private final Object localElement;

    /**
     * @param ancestor
     * @param localElement
     */
    protected LeftInheritanceTuple1(Tuple ancestor, Object localElement) {
        super(ancestor);
        this.localElement = localElement;
        calcHash();
    }

    /**
     * @return number of elements
     */
    public int getSize() {
        return inheritedIndex + 1;
    }

    @Override
    public int getLocalSize() {
        return 1;
    }

    /**
     * @pre: 0 <= index < getSize()
     *
     * @return the element at the specified index
     */
    public Object get(int index) {
        int local = index - inheritedIndex;
        if (local < 0)
            return ancestor.get(index);
        else if (local == 0) return localElement;
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
        if (localElement != null) cachedHash += localElement.hashCode();
    }

    /**
     * Optimized equals calculation (prediction: true, since hash values match)
     */
    @Override
    protected boolean localEquals(BaseLeftInheritanceTuple other) {
        if (other instanceof LeftInheritanceTuple1) {
            LeftInheritanceTuple1 lit = (LeftInheritanceTuple1)other;
            return Objects.equals(this.localElement, lit.localElement);
        } else {
            return (1 == other.getLocalSize()) &&
                    Objects.equals(localElement, other.get(inheritedIndex));
        }
    }

}
