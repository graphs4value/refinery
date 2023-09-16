/*******************************************************************************
 * Copyright (c) 2010-2015, Marton Bur, Zoltan Ujhelyi, Akos Horvath, Istvan Rath and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.interpreter.matchers.psystem.rewriters;

import tools.refinery.interpreter.matchers.psystem.basicenumerables.PositivePatternCall;


/**
 * Interface used by the PQueryFlattener to decide which positive pattern calls to flatten
 *
 * @author Marton Bur
 *
 */
public interface IFlattenCallPredicate {

    /**
     * Decides whether the called query by the pattern call should be flattened into the caller or not.
     *
     * @param positivePatternCall
     *            the pattern call
     * @return true if the call should be flattened
     */
    boolean shouldFlatten(PositivePatternCall positivePatternCall);

    /**
     * Flattens only if all operand predicates vote for flattening.
     * @author Gabor Bergmann
     * @since 2.1
     */
    public static class And implements IFlattenCallPredicate {
        private IFlattenCallPredicate[] operands;
        public And(IFlattenCallPredicate... operands) {
            this.operands = operands;
        }

        @Override
        public boolean shouldFlatten(PositivePatternCall positivePatternCall) {
            for (IFlattenCallPredicate operand : operands) {
                if (!operand.shouldFlatten(positivePatternCall)) return false;
            }
            return true;
        }
    }
}
