/*******************************************************************************
 * Copyright (c) 2010-2014, Bergmann Gabor, Istvan Rath and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.interpreter.matchers.planning.operations;

import java.util.Set;

import tools.refinery.interpreter.matchers.planning.SubPlan;
import tools.refinery.interpreter.matchers.psystem.PConstraint;
import tools.refinery.interpreter.matchers.util.Preconditions;

/**
 * Abstract superclass for representing a high-level query evaluation operation.
 *
 *  <p> Subclasses correspond to various POperations modeled after relational algebra.
 *
 * @author Bergmann Gabor
 *
 */
public abstract class POperation {

    /**
     * Newly enforced constraints
     */
    public abstract Set<? extends PConstraint> getDeltaConstraints();

    public abstract String getShortName();

    /**
     * @return the number of SubPlans that must be specified as parents
     */
    public abstract int numParentSubPlans();

    /**
     * Checks whether this constraint can be properly applied at the given SubPlan.
     */
    public void checkConsistency(SubPlan subPlan) {
        Preconditions.checkArgument(this == subPlan.getOperation(), "POperation misalignment");
        Preconditions.checkArgument(subPlan.getParentPlans().size() == numParentSubPlans(), "Incorrect number of parent SubPlans");
    }

    @Override
    public String toString() {
        return getShortName();
    }

}
