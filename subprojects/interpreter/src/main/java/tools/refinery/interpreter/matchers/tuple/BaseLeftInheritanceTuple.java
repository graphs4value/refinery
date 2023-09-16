/*******************************************************************************
 * Copyright (c) 2010-2017, Gabor Bergmann, IncQueryLabs Ltd.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.interpreter.matchers.tuple;

/**
 * Common functionality of left inheritance tuple implementations.
 *
 * <p>Left inheritance tuples inherit their first few elements from another tuple,
 * and extend it with additional "local" elements.
 *
 * @author Gabor Bergmann
 * @since 1.7
 */
public abstract class BaseLeftInheritanceTuple extends Tuple {

    /**
     * The number of elements that aren't stored locally, but inherited from an ancestor Tuple instead.
     */
    protected final int inheritedIndex;
    /**
     * This object contains the same elements as the ancestor on the first inheritedIndex positions
     */
    protected final Tuple ancestor;

    /**
     * @param ancestor
     */
    public BaseLeftInheritanceTuple(Tuple ancestor) {
        super();
        this.ancestor = ancestor;
        this.inheritedIndex = ancestor.getSize();
    }

    /**
     * @return the number of local (non-inherited) elements
     */
    public abstract int getLocalSize();

    /**
     * Optimized equals calculation (prediction: true, since hash values match)
     */
    @Override
    protected boolean internalEquals(ITuple other) {
        if (other instanceof BaseLeftInheritanceTuple) {
            BaseLeftInheritanceTuple blit = (BaseLeftInheritanceTuple) other;
            if (blit.inheritedIndex == this.inheritedIndex) {
                if (this.ancestor.equals(blit.ancestor)) {
                    return localEquals(blit);
                } else return false;
            }
        }
        return super.internalEquals(other);
    }

    /**
     * Checks the equivalence of local elements only, after ancestor tuple has been determined to be equal.
     */
    protected abstract boolean localEquals(BaseLeftInheritanceTuple other);
}
