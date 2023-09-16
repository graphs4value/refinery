/*******************************************************************************
 * Copyright (c) 2010-2018, Gabor Bergmann, IncQuery Labs Ltd.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.interpreter.matchers.util;

/**
 * The degree of accuracy of a cardinality estimate
 * @author Gabor Bergmann
 * @since 2.1
 */
public enum Accuracy {
    EXACT_COUNT,
    BEST_UPPER_BOUND,
    BEST_LOWER_BOUND,
    APPROXIMATION;

    /**
     * Partial order comparison.
     */
    public boolean atLeastAsPreciseAs(Accuracy other) {
        switch (this) {
        case EXACT_COUNT: return true;
        case APPROXIMATION: return APPROXIMATION == other;
        case BEST_UPPER_BOUND: return BEST_UPPER_BOUND == other || APPROXIMATION == other;
        case BEST_LOWER_BOUND: return BEST_LOWER_BOUND == other || APPROXIMATION == other;
        default: throw new IllegalArgumentException();
        }
    }

    /**
     * @return another accuracy value that is anti-monotonic to this one,
     * i.e. an accuracy that should be used in the denominator to obtain a fraction with this accuracy
     */
    public Accuracy reciprocal() {
        switch(this) {
        case APPROXIMATION: return APPROXIMATION;
        case BEST_UPPER_BOUND: return BEST_LOWER_BOUND;
        case BEST_LOWER_BOUND: return BEST_UPPER_BOUND;
        case EXACT_COUNT: return EXACT_COUNT;
        default: throw new IllegalArgumentException();
        }
    }
}
