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

import tools.refinery.interpreter.matchers.psystem.EnumerablePConstraint;
import tools.refinery.interpreter.matchers.psystem.PConstraint;

/**
 * Represents a base relation defined by the instance set of an enumerable PConstraint; there are no parent SubPlans.
 *
 * <p> <b>WARNING</b>: if there are coinciding variables in the variable tuple of the enumerable constraint,
 *   it is the responsibility of the compiler to check them for equality.
 * @author Bergmann Gabor
 *
 */
public class PEnumerate extends POperation {

    EnumerablePConstraint enumerablePConstraint;

    public PEnumerate(EnumerablePConstraint enumerablePConstraint) {
        super();
        this.enumerablePConstraint = enumerablePConstraint;
    }
    public EnumerablePConstraint getEnumerablePConstraint() {
        return enumerablePConstraint;
    }

    @Override
    public Set<? extends PConstraint> getDeltaConstraints() {
        return Collections.singleton(enumerablePConstraint);
    }
    @Override
    public int numParentSubPlans() {
        return 0;
    }
    @Override
    public String getShortName() {
        return enumerablePConstraint.toString();
    }
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime
                * result
                + ((enumerablePConstraint == null) ? 0 : enumerablePConstraint
                        .hashCode());
        return result;
    }
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (!(obj instanceof PEnumerate))
            return false;
        PEnumerate other = (PEnumerate) obj;
        if (enumerablePConstraint == null) {
            if (other.enumerablePConstraint != null)
                return false;
        } else if (!enumerablePConstraint.equals(other.enumerablePConstraint))
            return false;
        return true;
    }

}
