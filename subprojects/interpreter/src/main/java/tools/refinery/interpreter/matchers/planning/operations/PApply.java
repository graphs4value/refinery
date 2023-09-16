/*******************************************************************************
 * Copyright (c) 2010-2014, Bergmann Gabor, Istvan Rath and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.interpreter.matchers.planning.operations;

import java.util.Collections;
import java.util.Set;

import tools.refinery.interpreter.matchers.planning.SubPlan;
import tools.refinery.interpreter.matchers.psystem.PConstraint;
import tools.refinery.interpreter.matchers.util.Preconditions;

/**
 * Represents a constraint application on a single parent SubPlan.
 * <p> Either a "selection" filter operation according to a deferred PConstraint (or transform in case of eval/aggregate), or
 * alternatively a shorthand for PJoin + a PEnumerate on the right input for an enumerable PConstraint.
 *
 * <p> <b>WARNING</b>: if there are coinciding variables in the variable tuple of the enumerable constraint,
 *   it is the responsibility of the compiler to check them for equality.
 *
 * @author Bergmann Gabor
 *
 */
public class PApply extends POperation {

    private PConstraint pConstraint;

    public PApply(PConstraint pConstraint) {
        super();
        this.pConstraint = pConstraint;
    }
    public PConstraint getPConstraint() {
        return pConstraint;
    }

    @Override
    public String getShortName() {
        return String.format("APPLY_%s", pConstraint.toString());
    }

    @Override
    public Set<? extends PConstraint> getDeltaConstraints() {
        return Collections.singleton(pConstraint);
    }

    @Override
    public int numParentSubPlans() {
        return 1;
    }

    @Override
    public void checkConsistency(SubPlan subPlan) {
        super.checkConsistency(subPlan);
        for (SubPlan parentPlan : subPlan.getParentPlans())
            Preconditions.checkArgument(!parentPlan.getAllEnforcedConstraints().contains(pConstraint),
                    "Double-checking constraint %s", pConstraint);
        // TODO obtain context?
        //if (pConstraint instanceof DeferredPConstraint)
        //	Preconditions.checkArgument(((DeferredPConstraint) pConstraint).isReadyAt(subPlan, context))
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime
                * result
                + ((pConstraint == null) ? 0 : pConstraint
                        .hashCode());
        return result;
    }
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (!(obj instanceof PApply))
            return false;
        PApply other = (PApply) obj;
        if (pConstraint == null) {
            if (other.pConstraint != null)
                return false;
        } else if (!pConstraint.equals(other.pConstraint))
            return false;
        return true;
    }

}
