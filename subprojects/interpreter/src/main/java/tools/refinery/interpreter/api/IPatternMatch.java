/*******************************************************************************
 * Copyright (c) 2004-2010 Gabor Bergmann and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package tools.refinery.interpreter.api;

import java.util.List;

/**
 * Generic interface for a single match of a pattern. Each instance is a (partial) substitution of pattern parameters,
 * essentially a parameter to value mapping.
 *
 * <p>Can also represent a partial match; unsubstituted parameters are assigned to null. Pattern matchers must never return
 * a partial match, but they accept partial matches as method parameters.
 *
 * @author Bergmann Gábor
 */
public interface IPatternMatch extends Cloneable /* , Map<String, Object> */{
    /** @return the pattern for which this is a match. */
    public IQuerySpecification<? extends InterpreterMatcher<? extends IPatternMatch>> specification();

    /** Identifies the name of the pattern for which this is a match. */
    public String patternName();

    /** Returns the list of symbolic parameter names. */
    public List<String> parameterNames();

    /** Returns the value of the parameter with the given name, or null if name is invalid. */
    public Object get(String parameterName);

    /** Returns the value of the parameter at the given position, or null if position is invalid. */
    public Object get(int position);

    /**
     * Sets the parameter with the given name to the given value.
     *
     * <p> Works only if match is mutable. See {@link #isMutable()}.
     *
     * @returns true if successful, false if parameter name is invalid. May also fail and return false if the value type
     *          is incompatible.
     * @throws UnsupportedOperationException if match is not mutable.
     */
    public boolean set(String parameterName, Object newValue);

    /**
     * Sets the parameter at the given position to the given value.
     *
     * <p> Works only if match is mutable. See {@link #isMutable()}.
     *
     * @returns true if successful, false if position is invalid. May also fail and return false if the value type is
     *          incompatible.
     * @throws UnsupportedOperationException if match is not mutable.
     */
    public boolean set(int position, Object newValue);

    /**
     * Returns whether the match object can be further modified after its creation. Setters work only if the match is mutable.
     *
     * <p>Matches computed by the pattern matchers are not mutable, so that the match set cannot be modified externally.
     * Partial matches used as matcher input, however, can be mutable; such match objects can be created using {@link InterpreterMatcher#newEmptyMatch()}.
     *
     * @return whether the match can be modified
     */
    public boolean isMutable();

    /**
     * Converts the match to an array representation, with each pattern parameter at their respective position.
     * In case of a partial match, unsubstituted parameters will be represented as null elements in the array.
     *
     * @return a newly constructed array containing each parameter substitution of the match in order.
     */
    public Object[] toArray();

    /**
     * Takes an immutable snapshot of this match.
     * @return the match itself in case of immutable matches, an immutable copy in case of mutable ones.
     */
    public IPatternMatch toImmutable();

    /** Prints the list of parameter-value pairs. */
    public String prettyPrint();

    /**
     * Checks that this match is compatible with the given other match.
     * This is used for filtering the match set of a matcher.
     *
     * <p/> Two non-null matches are compatible if and only if:
     * <ul>
     *   <li>They share the same pattern.</li>
     *   <li>For each parameter, where they are set (non-null) in both matches,
     *    their values are equal.</li>
     *   </li>
     * </ul>
     *
     * <p/> Furthermore, all matches are considered compatible with
     *  null matches (e.g. empty filter).
     *
     * @param other
     * @return true, if this is compatible with other, or other is null
     */
    public boolean isCompatibleWith(IPatternMatch other);
}
