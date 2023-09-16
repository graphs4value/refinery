/*******************************************************************************
 * Copyright (c) 2004-2010 Gabor Bergmann and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package tools.refinery.interpreter.rete.construction.quasitree;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import tools.refinery.interpreter.matchers.planning.SubPlan;
import tools.refinery.interpreter.matchers.planning.SubPlanFactory;
import tools.refinery.interpreter.matchers.planning.helpers.FunctionalDependencyHelper;
import tools.refinery.interpreter.matchers.planning.operations.PJoin;
import tools.refinery.interpreter.matchers.psystem.PConstraint;
import tools.refinery.interpreter.matchers.psystem.PVariable;
import tools.refinery.interpreter.matchers.psystem.analysis.QueryAnalyzer;
import tools.refinery.interpreter.matchers.util.CollectionsFactory;

/**
 * @author Gabor Bergmann
 *
 */
class JoinCandidate {
    private QueryAnalyzer analyzer;

    SubPlan primary;
    SubPlan secondary;

    Set<PVariable> varPrimary;
    Set<PVariable> varSecondary;
    Set<PVariable> varCommon;

    List<PConstraint> consPrimary;
    List<PConstraint> consSecondary;


    JoinCandidate(SubPlan primary, SubPlan secondary, QueryAnalyzer analyzer) {
        super();
        this.primary = primary;
        this.secondary = secondary;
        this.analyzer = analyzer;

        varPrimary = getPrimary().getVisibleVariables();
        varSecondary = getSecondary().getVisibleVariables();
        varCommon = CollectionsFactory.createSet(varPrimary);
        varCommon.retainAll(varSecondary);

        consPrimary = new ArrayList<PConstraint>(primary.getAllEnforcedConstraints());
        Collections.sort(consPrimary, TieBreaker.CONSTRAINT_COMPARATOR);
        consSecondary = new ArrayList<PConstraint>(secondary.getAllEnforcedConstraints());
        Collections.sort(consSecondary, TieBreaker.CONSTRAINT_COMPARATOR);
    }



    /**
     * @return the a
     */
    public SubPlan getPrimary() {
        return primary;
    }

    /**
     * @return the b
     */
    public SubPlan getSecondary() {
        return secondary;
    }

    public SubPlan getJoinedPlan(SubPlanFactory factory) {
        // check special cases first
        if (isTrivial())
            return primary;
        if (isSubsumption())
            return
                (consPrimary.size() > consSecondary.size()) ? primary : secondary;


        // default case
        return factory.createSubPlan(new PJoin(), primary, secondary);
    }

    @Override
    public String toString() {
        return primary.toString() + " |x| " + secondary.toString();
    }

    /**
     * @return the varPrimary
     */
    public Set<PVariable> getVarPrimary() {
        return varPrimary;
    }

    /**
     * @return the varSecondary
     */
    public Set<PVariable> getVarSecondary() {
        return varSecondary;
    }

    /**
     * @return constraints of primary, sorted according to {@link TieBreaker#CONSTRAINT_COMPARATOR}.
     */
    public List<PConstraint> getConsPrimary() {
        return consPrimary;
    }
    /**
     * @return constraints of secondary, sorted according to {@link TieBreaker#CONSTRAINT_COMPARATOR}.
     */
    public List<PConstraint> getConsSecondary() {
        return consSecondary;
    }



    public boolean isTrivial() {
        return getPrimary().equals(getSecondary());
    }

    public boolean isSubsumption() {
        return consPrimary.containsAll(consSecondary) || consSecondary.containsAll(consPrimary);
    }

    public boolean isCheckOnly() {
        return varPrimary.containsAll(varSecondary) || varSecondary.containsAll(varPrimary);
    }

    public boolean isDescartes() {
        return Collections.disjoint(varPrimary, varSecondary);
    }

    private Boolean heath;

    // it is a Heath-join iff common variables functionally determine either all primary or all secondary variables
    public boolean isHeath() {
        if (heath == null) {
            Set<PConstraint> union = Stream.concat(
                    primary.getAllEnforcedConstraints().stream(),
                    secondary.getAllEnforcedConstraints().stream()
            ).collect(Collectors.toSet());
            Map<Set<PVariable>, Set<PVariable>> dependencies =
                    analyzer.getFunctionalDependencies(union, false);
            // does varCommon determine either varPrimary or varSecondary?
            Set<PVariable> varCommonClosure = FunctionalDependencyHelper.closureOf(varCommon, dependencies);

            heath = varCommonClosure.containsAll(varPrimary) || varCommonClosure.containsAll(varSecondary);
        }
        return heath;
    }

}
