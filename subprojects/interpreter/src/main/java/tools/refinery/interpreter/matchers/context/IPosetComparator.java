/*******************************************************************************
 * Copyright (c) 2010-2016, Tamas Szabo, Istvan Rath and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.interpreter.matchers.context;

import tools.refinery.interpreter.matchers.tuple.Tuple;

/**
 * Implementations of this interface aid the query engine with the ordering of poset elements. This information is
 * particularly important in the delete and re-derive evaluation mode because they let the engine identify monotone
 * change pairs.
 *
 * @author Tamas Szabo
 * @since 1.6
 */
public interface IPosetComparator {

    /**
     * Returns true if the 'left' tuple of poset elements is smaller or equal than the 'right' tuple of poset elements according to
     * the partial order that this poset comparator employs.
     *
     * @param left
     *            the left tuple of poset elements
     * @param right
     *            the right tuple of poset elements
     * @return true if left is smaller or equal to right, false otherwise
     */
    public boolean isLessOrEqual(final Tuple left, final Tuple right);

}
