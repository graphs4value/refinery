/*******************************************************************************
 * Copyright (c) 2004-2010 Gabor Bergmann and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package tools.refinery.interpreter.rete.construction.quasitree;

import java.util.Comparator;

import tools.refinery.interpreter.rete.util.Options;
import tools.refinery.interpreter.rete.util.OrderingCompareAgent;

/**
 * @author Gabor Bergmann
 *
 */
public class JoinOrderingHeuristics implements Comparator<JoinCandidate> {

    @Override
    public int compare(JoinCandidate jc1, JoinCandidate jc2) {
        return new OrderingCompareAgent<JoinCandidate>(jc1, jc2) {
            @Override
            protected void doCompare() {
                swallowBoolean(true && consider(preferTrue(a.isTrivial(), b.isTrivial()))
                        && consider(preferTrue(a.isSubsumption(), b.isSubsumption()))
                        && consider(preferTrue(a.isCheckOnly(), b.isCheckOnly()))
                        && consider(
                                Options.functionalDependencyOption == Options.FunctionalDependencyOption.OFF ?
                                dontCare() :
                                preferTrue(a.isHeath(), b.isHeath())
                            )
                        && consider(preferFalse(a.isDescartes(), b.isDescartes()))

                        // TODO main heuristic decisions

                        // tie breaking
                        && consider(preferLess(a.getConsPrimary(), b.getConsPrimary(), TieBreaker.CONSTRAINT_LIST_COMPARATOR))
                        && consider(preferLess(a.getConsSecondary(), b.getConsSecondary(), TieBreaker.CONSTRAINT_LIST_COMPARATOR))
                        && consider(preferLess(System.identityHashCode(a), System.identityHashCode(b))));
            }
        }.compare();

    }

}
