/*******************************************************************************
 * Copyright (c) 2004-2010 Gabor Bergmann and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package tools.refinery.interpreter.api;

import java.util.Arrays;

import tools.refinery.interpreter.api.impl.BasePatternMatch;

/**
 * Generic signature object implementation.
 *
 * See also the generated matcher and signature of the pattern, with pattern-specific API simplifications.
 *
 * @author Bergmann Gábor
 * @since 0.9
 *
 */
public abstract class GenericPatternMatch extends BasePatternMatch {

    private final GenericQuerySpecification<? extends GenericPatternMatcher> specification;
    private final Object[] array;

    private GenericPatternMatch(GenericQuerySpecification<? extends GenericPatternMatcher> specification, Object[] array) {
        super();
        this.specification = specification;
        this.array = array;
    }

    @Override
    public Object get(String parameterName) {
        Integer index = specification.getPositionOfParameter(parameterName);
        return index == null ? null : array[index];
    }

    @Override
    public Object get(int position) {
        return array[position];
    }

    @Override
    public boolean set(String parameterName, Object newValue) {
        if (!isMutable()) throw new UnsupportedOperationException();
        Integer index = specification.getPositionOfParameter(parameterName);
        if (index == null)
            return false;
        array[index] = newValue;
        return true;
    }

    @Override
    public Object[] toArray() {
        return Arrays.copyOf(array, array.length);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        for (int i = 0; i < array.length; ++i)
            result = prime * result + ((array[i] == null) ? 0 : array[i].hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!(obj instanceof GenericPatternMatch)) { // this should be infrequent
            if (obj == null)
                return false;
            if (!(obj instanceof IPatternMatch))
                return false;
            IPatternMatch other = (IPatternMatch) obj;
            if (!specification().equals(other.specification()))
                return false;
            return Arrays.deepEquals(array, other.toArray());
        }
        final GenericPatternMatch other = (GenericPatternMatch) obj;
        return specification().equals(other.specification()) && Arrays.deepEquals(array, other.array);
    }

    @Override
    public String prettyPrint() {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < array.length; ++i) {
            if (i != 0)
                result.append(", ");
            result.append("\"" + parameterNames().get(i) + "\"=" + prettyPrintValue(array[i]));
        }
        return result.toString();
    }

    @Override
    public GenericQuerySpecification<? extends GenericPatternMatcher> specification() {
        return specification;
    }

    /**
     * Returns an empty, mutable match.
     * Fields of the mutable match can be filled to create a partial match, usable as matcher input.
     *
     * @return the empty match
     */
    public static GenericPatternMatch newEmptyMatch(GenericQuerySpecification<? extends GenericPatternMatcher> specification) {
        return new Mutable(specification, new Object[specification.getParameters().size()]);
    }

    /**
     * Returns a mutable (partial) match.
     * Fields of the mutable match can be filled to create a partial match, usable as matcher input.
     *
     * @param parameters
     *            the fixed value of pattern parameters, or null if not bound.
     * @return the new, mutable (partial) match object.
     */
    public static GenericPatternMatch newMutableMatch(GenericQuerySpecification<? extends GenericPatternMatcher> specification, Object... parameters) {
        return new Mutable(specification, parameters);
    }

    /**
     * Returns a new (partial) match.
     * This can be used e.g. to call the matcher with a partial match.
     *
     * <p>The returned match will be immutable. Use {@link #newEmptyMatch(GenericQuerySpecification)} to obtain a mutable match object.
     *
     * @param parameters
     *            the fixed value of pattern parameters, or null if not bound.
     * @return the (partial) match object.
     */
    public static GenericPatternMatch newMatch(GenericQuerySpecification<? extends GenericPatternMatcher> specification, Object... parameters) {
        return new Immutable(specification, Arrays.copyOf(parameters, parameters.length));
    }

    @Override
    public IPatternMatch toImmutable() {
        return isMutable() ? newMatch(specification, array) : this;
    }

    static final class Mutable extends GenericPatternMatch {
        Mutable(GenericQuerySpecification<? extends GenericPatternMatcher> specification, Object[] array) {
            super(specification, array);
        }

        @Override
        public boolean isMutable() {
            return true;
        }
    }
    static final class Immutable extends GenericPatternMatch {
        Immutable(GenericQuerySpecification<? extends GenericPatternMatcher> specification, Object[] array) {
            super(specification, array);
        }

        @Override
        public boolean isMutable() {
            return false;
        }
    }
}
