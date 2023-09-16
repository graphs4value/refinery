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

import tools.refinery.interpreter.matchers.psystem.PConstraint;

/**
 * Represents a natural join of two parent SubPlans.
 * @author Bergmann Gabor
 *
 */
public class PJoin extends POperation {

//	// TODO leave here? is this a problem in equivalnece checking?
//	private Set<PVariable> onVariables;

    public PJoin(/*Set<PVariable> onVariables*/) {
        super();
        //this.onVariables = new HashSet<PVariable>(onVariables);
    }
//	public Set<PVariable> getOnVariables() {
//		return onVariables;
//	}

    @Override
    public Set<? extends PConstraint> getDeltaConstraints() {
        return Collections.emptySet();
    }
    @Override
    public int numParentSubPlans() {
        return 2;
    }

    @Override
    public String getShortName() {
        return "JOIN"; //String.format("JOIN_{%s}", Joiner.on(",").join(onVariables));
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (!(obj instanceof PJoin))
            return false;
        return true;
    }


}
