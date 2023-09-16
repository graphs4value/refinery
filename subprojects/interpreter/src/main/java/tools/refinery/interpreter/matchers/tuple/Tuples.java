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
 * Common static factory utilities for tuples.
 *
 * @author Gabor Bergmann
 * @since 1.7
 */
public class Tuples {

    private Tuples() {
        // Empty utility class constructor
    }

    /**
     * Creates a flat tuple consisting of the given elements.
     * For low-arity tuples, specialized implementations
     * (such as {@link FlatTuple2}) will be instantiated.
     *
     * <p> In case the exact arity is <i>statically</i> known,
     * it may be more efficient for the client to instantiate
     * the appropriate specialized implementation
     * (via {@link #staticArityFlatTupleOf(Object, Object)} etc.
     * or {@link #wideFlatTupleOf(Object...)}),
     * instead of invoking this method.
     * This method does a runtime arity check, and therefore
     * also appropriate if the arity is determined at runtime.
     */
    public static Tuple flatTupleOf(Object... elements) {
        switch (elements.length) {
        case 0:
            return FlatTuple0.INSTANCE;
        case 1:
            return new FlatTuple1(elements[0]);
        case 2:
            return new FlatTuple2(elements[0], elements[1]);
        case 3:
            return new FlatTuple3(elements[0], elements[1], elements[2]);
        case 4:
            return new FlatTuple4(elements[0], elements[1], elements[2], elements[3]);
        default:
            return new FlatTuple(elements);
        }
    }
    /**
     * Creates a left inheritance tuple that extends an ancestor tuple
     *  by the given "local" elements.
     * For locally low-arity tuples, specialized implementations
     * (such as {@link LeftInheritanceTuple2}) will be instantiated.
     *
     * <p> In case the exact arity is <i>statically</i> known,
     * it may be more efficient for the client to instantiate
     * the appropriate specialized implementation
     * (via {@link #staticArityLeftInheritanceTupleOf(Object, Object)} etc.
     * or {@link #wideLeftInheritanceTupleOf(Object...)}),
     * instead of invoking this method.
     * This method does a runtime arity check, and therefore
     * also appropriate if the arity is determined at runtime.
     */
    public static Tuple leftInheritanceTupleOf(Tuple ancestor, Object... localElements) {
        switch (localElements.length) {
        case 0:
            return ancestor;
        case 1:
            return new LeftInheritanceTuple1(ancestor, localElements[0]);
        case 2:
            return new LeftInheritanceTuple2(ancestor, localElements[0], localElements[1]);
        case 3:
            return new LeftInheritanceTuple3(ancestor, localElements[0], localElements[1], localElements[2]);
        case 4:
            return new LeftInheritanceTuple4(ancestor, localElements[0], localElements[1], localElements[2], localElements[3]);
        default:
            return new LeftInheritanceTuple(ancestor, localElements);
        }
    }

    /**
     * Creates a flat tuple consisting of no elements.
     */
    public static Tuple staticArityFlatTupleOf() {
        return FlatTuple0.INSTANCE;
    }
    /**
     * Creates a flat tuple consisting of the given single element.
     */
    public static Tuple staticArityFlatTupleOf(Object element) {
        return new FlatTuple1(element);
    }
    /**
     * Creates a flat tuple consisting of the given elements.
     */
    public static Tuple staticArityFlatTupleOf(Object element0, Object element1) {
        return new FlatTuple2(element0, element1);
    }
    /**
     * Creates a flat tuple consisting of the given elements.
     */
    public static Tuple staticArityFlatTupleOf(Object element0, Object element1, Object element2) {
        return new FlatTuple3(element0, element1, element2);
    }
    /**
     * Creates a flat tuple consisting of the given elements.
     */
    public static Tuple staticArityFlatTupleOf(Object element0, Object element1, Object element2, Object element3) {
        return new FlatTuple4(element0, element1, element2, element3);
    }
    /**
     * Creates a flat tuple consisting of the given elements.
     * <p> Invoke this only if it is statically known that the tuple will be wide.
     * Otherwise, use {@link #flatTupleOf(Object...)}.
     */
    public static Tuple wideFlatTupleOf(Object... elements) {
        return new FlatTuple(elements);
    }

    /**
     * Creates a left inheritance tuple consisting of the given single local element.
     */
    public static Tuple staticArityLeftInheritanceTupleOf(Tuple ancestor, Object element) {
        return new LeftInheritanceTuple1(ancestor, element);
    }
    /**
     * Creates a left inheritance tuple consisting of the given local elements.
     */
    public static Tuple staticArityLeftInheritanceTupleOf(Tuple ancestor, Object element0, Object element1) {
        return new LeftInheritanceTuple2(ancestor, element0, element1);
    }
    /**
     * Creates a left inheritance tuple consisting of the given local elements.
     */
    public static Tuple staticArityLeftInheritanceTupleOf(Tuple ancestor, Object element0, Object element1, Object element2) {
        return new LeftInheritanceTuple3(ancestor, element0, element1, element2);
    }
    /**
     * Creates a left inheritance tuple consisting of the given local elements.
     */
    public static Tuple staticArityLeftInheritanceTupleOf(Tuple ancestor, Object element0, Object element1, Object element2, Object element3) {
        return new LeftInheritanceTuple4(ancestor, element0, element1, element2, element3);
    }
    /**
     * Creates a left inheritance tuple consisting of the given local elements.
     * <p> Invoke this only if it is statically known that the tuple will be wide.
     * Otherwise, use {@link #leftInheritanceTupleOf(Tuple, Object...)}.
     */
    public static Tuple wideLeftInheritanceTupleOf(Tuple ancestor, Object... elements) {
        return new LeftInheritanceTuple(ancestor, elements);
    }

}
