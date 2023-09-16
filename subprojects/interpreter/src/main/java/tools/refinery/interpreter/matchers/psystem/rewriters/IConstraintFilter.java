/*******************************************************************************
 * Copyright (c) 2010-2015, Zoltan Ujhelyi, Marton Bur, Istvan Rath and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.interpreter.matchers.psystem.rewriters;

import tools.refinery.interpreter.matchers.psystem.basicdeferred.ExportedParameter;
import tools.refinery.interpreter.matchers.psystem.PConstraint;

/**
 * Helper interface to exclude constraints from PBody copy processes
 *
 * @author Marton Bur
 *
 */
public interface IConstraintFilter {
    /**
     * Returns true, if the given constraint should be filtered (thus should not be copied)
     *
     * @param constraint
     *            to check
     * @return true, if the constraint should be filtered
     */
    boolean filter(PConstraint constraint);

    public static class ExportedParameterFilter implements IConstraintFilter {

        @Override
        public boolean filter(PConstraint constraint) {
            return constraint instanceof ExportedParameter;
        }

    }

    public static class AllowAllFilter implements IConstraintFilter {

        @Override
        public boolean filter(PConstraint constraint) {
            // Nothing is filtered
            return false;
        }

    }
}
