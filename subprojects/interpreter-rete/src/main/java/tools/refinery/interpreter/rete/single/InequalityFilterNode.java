/*******************************************************************************
 * Copyright (c) 2004-2008 Gabor Bergmann and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package tools.refinery.interpreter.rete.single;

import tools.refinery.interpreter.rete.network.ReteContainer;
import tools.refinery.interpreter.matchers.tuple.Tuple;
import tools.refinery.interpreter.matchers.tuple.TupleMask;

/**
 * This node filters patterns according to equalities and inequalities of elements. The 'subject' element is asserted to
 * be different from the elements given by the inequalityMask.
 *
 *
 * @author Gabor Bergmann
 *
 */
public class InequalityFilterNode extends FilterNode {

    int subjectIndex;
    TupleMask inequalityMask;

    /**
     * @param reteContainer
     * @param subject
     *            the index of the element that should be compared.
     * @param inequalityMask
     *            the indices of elements that should be different from the subjectIndex.
     */
    public InequalityFilterNode(ReteContainer reteContainer, int subject, TupleMask inequalityMask) {
        super(reteContainer);
        this.subjectIndex = subject;
        this.inequalityMask = inequalityMask;
    }

    @Override
    public boolean check(Tuple ps) {
        Object subject = ps.get(subjectIndex);
        for (int ineq : inequalityMask.indices) {
            if (subject.equals(ps.get(ineq)))
                return false;
        }
        return true;
    }

}
