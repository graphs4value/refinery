/*******************************************************************************
 * Copyright (c) 2010-2014, Bergmann Gabor, Istvan Rath and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.interpreter.matchers.planning.operations;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import tools.refinery.interpreter.matchers.psystem.PConstraint;
import tools.refinery.interpreter.matchers.psystem.PVariable;

/**
 * No constraints, and no parent SubPlan, just a (possibly empty) set of a priori known (input) variables. Satisfied by a single tuple.
 *
 * <p> Can also be used without a priori variables,
 *   e.g. as a "virtual parent" in extreme cases,
 *   such as <code>pattern foo(Bar) = {Bar = eval (3*4)} </code>
 *
 * @author Bergmann Gabor
 *
 */
public class PStart extends POperation {

    private Set<PVariable> aPrioriVariables;


    public PStart(Set<PVariable> aPrioriVariables) {
        super();
        this.aPrioriVariables = aPrioriVariables;
    }
    public PStart(PVariable... aPrioriVariables) {
        this(new HashSet<PVariable>(Arrays.asList(aPrioriVariables)));
    }
    public Set<PVariable> getAPrioriVariables() {
        return aPrioriVariables;
    }

    @Override
    public String getShortName() {
        return aPrioriVariables.stream().map(PVariable::getName).collect(Collectors.joining(",", "START_{", "}"));
    }
    @Override
    public int numParentSubPlans() {
        return 0;
    }

    @Override
    public Set<? extends PConstraint> getDeltaConstraints() {
        return Collections.emptySet();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime
                * result
                + ((aPrioriVariables == null) ? 0 : aPrioriVariables.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (!(obj instanceof PStart))
            return false;
        PStart other = (PStart) obj;
        if (aPrioriVariables == null) {
            if (other.aPrioriVariables != null)
                return false;
        } else if (!aPrioriVariables.equals(other.aPrioriVariables))
            return false;
        return true;
    }




}
