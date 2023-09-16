/*******************************************************************************
 * Copyright (c) 2004-2010 Gabor Bergmann and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package tools.refinery.interpreter.api.impl;

import tools.refinery.interpreter.api.IPatternMatch;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Base implementation of IPatternMatch.
 *
 * @author Bergmann Gábor
 *
 */
public abstract class BasePatternMatch implements IPatternMatch {

    @SafeVarargs
    protected static <T> List<T> makeImmutableList(T... elements) {
        return Collections.unmodifiableList(Arrays.asList(elements));
    }

    public static String prettyPrintValue(Object o) {
        if (o == null) {
            return "(null)";
        }
        return o.toString();
    }

    // TODO performance can be improved here somewhat

    @Override
    public Object get(int position) {
        if (position >= 0 && position < parameterNames().size())
            return get(parameterNames().get(position));
        else
            return null;
    }

    @Override
    public boolean set(int position, Object newValue) {
        if (!isMutable()) throw new UnsupportedOperationException();
        if (position >= 0 && position < parameterNames().size()) {
            return set(parameterNames().get(position), newValue);
        } else {
            return false;
        }
    }

    @Override
    public String toString() {
        return "Match<" + patternName() + ">{" + prettyPrint() + "}";
    }

    @Override
    public boolean isCompatibleWith(IPatternMatch other) {
        if(other == null) {
            return true;
        }
        // we assume that the pattern is set for this match!
        if (!specification().equals(other.specification())) {
            return false;
        }
        for (int i = 0; i < parameterNames().size(); i++) {
            Object value = get(i);
            Object otherValue = other.get(i);
            if(value != null && otherValue != null && !value.equals(otherValue)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public String patternName() {
        return specification().getFullyQualifiedName();
    }

    @Override
    public List<String> parameterNames() {
        return specification().getParameterNames();
    }
}
