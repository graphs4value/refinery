/*******************************************************************************
 * Copyright (c) 2010-2014, Bergmann Gabor, Istvan Rath and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.interpreter.rete.construction.quasitree;

import java.util.Comparator;

import tools.refinery.interpreter.matchers.psystem.PConstraint;
import tools.refinery.interpreter.rete.util.LexicographicComparator;

/**
 * Class providing comparators for breaking ties somewhat more deterministically.
 * @author Bergmann Gabor
 *
 */
public class TieBreaker {

    private TieBreaker() {/*Utility class constructor*/}

    public static final Comparator<PConstraint> CONSTRAINT_COMPARATOR = (arg0, arg1) -> arg0.getMonotonousID() - arg1.getMonotonousID();

    public static final Comparator<Iterable<? extends PConstraint>> CONSTRAINT_LIST_COMPARATOR =
            new LexicographicComparator<PConstraint>(CONSTRAINT_COMPARATOR);

}
