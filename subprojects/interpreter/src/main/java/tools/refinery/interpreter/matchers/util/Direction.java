/*******************************************************************************
 * Copyright (c) 2010-2019, Tamas Szabo, itemis AG, Gabor Bergmann, IncQuery Labs Ltd.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.interpreter.matchers.util;

/**
 * Indicates whether a propagated update event signals the insertion or deletion of an element
 *
 * @author Gabor Bergmann
 */
public enum Direction {
    INSERT, DELETE;

    /**
     * @since 2.4
     */
    public Direction opposite() {
        switch (this) {
        case INSERT:
            return DELETE;
        default:
            return INSERT;
        }
    }

    /**
     * @since 2.4
     */
    public char asSign() {
        switch (this) {
        case INSERT:
            return '+';
        default:
            return '-';
        }
    }

    /**
     * Returns the direction that is the product of this direction and the other direction.
     *
     * DELETE x DELETE = INSERT
     * DELETE x INSERT = DELETE
     * INSERT x DELETE = DELETE
     * INSERT x INSERT = INSERT
     * @since 2.4
     */
    public Direction multiply(final Direction other) {
        switch (this) {
        case DELETE:
            return other.opposite();
        default:
            return other;
        }
    }

}
