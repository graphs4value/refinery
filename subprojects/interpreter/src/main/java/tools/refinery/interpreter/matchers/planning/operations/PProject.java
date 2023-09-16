/*******************************************************************************
 * Copyright (c) 2010-2014, Bergmann Gabor, Istvan Rath and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.interpreter.matchers.planning.operations;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import tools.refinery.interpreter.matchers.planning.SubPlan;
import tools.refinery.interpreter.matchers.psystem.PConstraint;
import tools.refinery.interpreter.matchers.psystem.PVariable;
import tools.refinery.interpreter.matchers.util.Preconditions;

/**
 * Represents a projection of a single parent SubPlan onto a limited set of variables.
 * <p> May optionally prescribe an ordering of variables (List, as opposed to Set).
 *
 * @author Bergmann Gabor
 *
 */
public class PProject extends POperation {

    private Collection<PVariable> toVariables;
    private boolean ordered;


    public PProject(Set<PVariable> toVariables) {
        super();
        this.toVariables = toVariables;
        this.ordered = false;
    }
    public PProject(List<PVariable> toVariables) {
        super();
        this.toVariables = toVariables;
        this.ordered = true;
    }

    public Collection<PVariable> getToVariables() {
        return toVariables;
    }
    public boolean isOrdered() {
        return ordered;
    }

    @Override
    public Set<? extends PConstraint> getDeltaConstraints() {
        return Collections.emptySet();
    }
    @Override
    public int numParentSubPlans() {
        return 1;
    }
    @Override
    public void checkConsistency(SubPlan subPlan) {
        super.checkConsistency(subPlan);
        final SubPlan parentPlan = subPlan.getParentPlans().get(0);

        Preconditions.checkArgument(parentPlan.getVisibleVariables().containsAll(toVariables),
                () -> toVariables.stream()
                        .filter(input -> !parentPlan.getVisibleVariables().contains(input)).map(PVariable::getName)
                        .collect(Collectors.joining(",", "Variables missing from project: ", "")));
    }

    @Override
    public String getShortName() {
        return String.format("PROJECT%s_{%s}", ordered? "!" : "",
                toVariables.stream().map(PVariable::getName).collect(Collectors.joining(",")));
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (ordered ? 1231 : 1237);
        result = prime * result
                + ((toVariables == null) ? 0 : toVariables.hashCode());
        return result;
    }
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (!(obj instanceof PProject))
            return false;
        PProject other = (PProject) obj;
        if (ordered != other.ordered)
            return false;
        if (toVariables == null) {
            if (other.toVariables != null)
                return false;
        } else if (!toVariables.equals(other.toVariables))
            return false;
        return true;
    }




}
