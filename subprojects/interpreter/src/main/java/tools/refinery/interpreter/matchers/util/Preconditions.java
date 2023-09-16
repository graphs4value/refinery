/*******************************************************************************
 * Copyright (c) 2010-2018, Zoltan Ujhelyi, IncQuery Labs Ltd.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.interpreter.matchers.util;

import java.util.function.Supplier;

/**
 * This class was motivated by the similar Preconditions class from Guava to provide simple precondition checking
 * functionality. However, as starting with version 2.0 the runtime of VIATRA Query should not depend on Guava, the
 * relevant functionality of the Preconditions checking functionality will be implemented here.
 *
 * @author Zoltan Ujhelyi
 * @since 2.0
 *
 */
public final class Preconditions {

    private Preconditions() {
        /* Utility class constructor */ }

    /**
     * Ensures the truth of an expression involving one or more parameters to the calling method.
     *
     * @param expression
     *            a boolean expression
     * @throws IllegalArgumentException
     *             if {@code expression} is false
     */
    public static void checkArgument(boolean expression) {
        if (!expression) {
            throw new IllegalArgumentException();
        }
    }

    /**
     * Ensures the truth of an expression involving one or more parameters to the calling method.
     *
     * @param expression
     *            a boolean expression
     * @param errorMessage
     *            the exception message to use if the check fails
     * @throws IllegalArgumentException
     *             if {@code expression} is false
     */
    public static void checkArgument(boolean expression, String errorMessage) {
        if (!expression) {
            throw new IllegalArgumentException(errorMessage);
        }
    }

    /**
     * Ensures the truth of an expression involving one or more parameters to the calling method.
     *
     * @param expression
     *            a boolean expression
     * @param errorMessageTemplate
     *            a template for the exception message should the check fail using the Java Formatter syntax; the same
     *            as used by {@link String#format(String, Object...)}.
     * @param errorMessageArgs
     *            the arguments to be substituted into the message template.
     * @throws IllegalArgumentException
     *             if {@code expression} is false
     * @throws NullPointerException
     *             if the check fails and either {@code errorMessageTemplate} or {@code errorMessageArgs} is null (don't
     *             let this happen)
     */
    public static void checkArgument(boolean expression, String errorMessageTemplate, Object... errorMessageArgs) {
        if (!expression) {
            throw new IllegalArgumentException(String.format(errorMessageTemplate, errorMessageArgs));
        }
    }

    /**
     * Ensures the truth of an expression involving one or more parameters to the calling method.
     *
     * @param expression
     *            a boolean expression
     * @param messageSupplier a supplier that is called to calculate the error message if necessary
     * @throws IllegalArgumentException
     *             if {@code expression} is false
     */
    public static void checkArgument(boolean expression, Supplier<String> messageSupplier) {
        if (!expression) {
            throw new IllegalArgumentException(messageSupplier.get());
        }
    }

    /**
     * Ensures the truth of an expression involving one or more fields of a class.
     *
     * @param expression
     *            a boolean expression
     * @throws IllegalStateException
     *             if {@code expression} is false
     */
    public static void checkState(boolean expression) {
        if (!expression) {
            throw new IllegalStateException();
        }
    }

    /**
     * Ensures the truth of an expression involving one or more fields of a class.
     *
     * @param expression
     *            a boolean expression
     * @param errorMessage
     *            the exception message to use if the check fails
     * @throws IllegalStateException
     *             if {@code expression} is false
     */
    public static void checkState(boolean expression, String errorMessage) {
        if (!expression) {
            throw new IllegalStateException(errorMessage);
        }
    }

    /**
     * Ensures the truth of an expression involving one or more fields of a class.
     *
     * @param expression
     *            a boolean expression
     * @param errorMessageTemplate
     *            a template for the exception message should the check fail using the Java Formatter syntax; the same
     *            as used by {@link String#format(String, Object...)}.
     * @param errorMessageArgs
     *            the arguments to be substituted into the message template.
     * @throws IllegalStateException
     *             if {@code expression} is false
     * @throws NullPointerException
     *             if the check fails and either {@code errorMessageTemplate} or {@code errorMessageArgs} is null (don't
     *             let this happen)
     */
    public static void checkState(boolean expression, String errorMessageTemplate, Object... errorMessageArgs) {
        if (!expression) {
            throw new IllegalStateException(String.format(errorMessageTemplate, errorMessageArgs));
        }
    }

    /**
     * Ensures the truth of an expression involving one or more fields of a class.
     *
     * @param expression
     *            a boolean expression
     * @param messageSupplier a supplier that is called to calculate the error message if necessary
     * @throws IllegalStateException
     *             if {@code expression} is false
     */
    public static void checkState(boolean expression, Supplier<String> messageSupplier) {
        if (!expression) {
            throw new IllegalStateException(messageSupplier.get());
        }
    }

    /**
     * Ensures that an index is appropriate for a list or array of given size.
     *
     * @param index
     * @param size
     * @throws IndexOutOfBoundsException
     *             if index is negative or is greater or equal to size
     */
    public static void checkElementIndex(int index, int size) {
        if (index < 0 || index >= size) {
            throw new IndexOutOfBoundsException();
        }
    }

    /**
     * Ensures that an index is appropriate for a list or array of given size.
     *
     * @param index
     * @param size
     * @param errorMessageTemplate
     *            a template for the exception message should the check fail using the Java Formatter syntax; the same
     *            as used by {@link String#format(String, Object...)}.
     * @param errorMessageArgs
     *            the arguments to be substituted into the message template.
     * @throws IndexOutOfBoundsException
     *             if index is negative or is greater or equal to size
     */
    public static void checkElementIndex(int index, int size, String errorMessageTemplate, Object... errorMessageArgs) {
        if (index < 0 || index >= size) {
            throw new IndexOutOfBoundsException(String.format(errorMessageTemplate, errorMessageArgs));
        }
    }

    /**
     * Ensures that an index is appropriate for a list or array of given size.
     *
     * @param index
     * @param size
     * @param messageSupplier a supplier that is called to calculate the error message if necessary
     * @throws IndexOutOfBoundsException
     *             if index is negative or is greater or equal to size
     */
    public static void checkElementIndex(int index, int size, Supplier<String> messageSupplier) {
        if (index < 0 || index >= size) {
            throw new IndexOutOfBoundsException(messageSupplier.get());
        }
    }
}
