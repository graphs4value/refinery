/*******************************************************************************
 * Copyright (c) 2004-2010 Gabor Bergmann and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package tools.refinery.interpreter.matchers.planning.helpers;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import tools.refinery.interpreter.matchers.planning.operations.PProject;
import tools.refinery.interpreter.matchers.InterpreterRuntimeException;
import tools.refinery.interpreter.matchers.context.IQueryMetaContext;
import tools.refinery.interpreter.matchers.planning.QueryProcessingException;
import tools.refinery.interpreter.matchers.planning.SubPlan;
import tools.refinery.interpreter.matchers.planning.SubPlanFactory;
import tools.refinery.interpreter.matchers.psystem.PBody;
import tools.refinery.interpreter.matchers.psystem.PConstraint;
import tools.refinery.interpreter.matchers.psystem.PVariable;
import tools.refinery.interpreter.matchers.psystem.analysis.QueryAnalyzer;
import tools.refinery.interpreter.matchers.psystem.basicdeferred.ExportedParameter;

/**
 * @author Gabor Bergmann
 *
 */
public class BuildHelper {

    private BuildHelper() {
        // Hiding constructor for utility class
    }

//    public static SubPlan naturalJoin(IOperationCompiler buildable,
//            SubPlan primaryPlan, SubPlan secondaryPlan) {
//        JoinHelper joinHelper = new JoinHelper(primaryPlan, secondaryPlan);
//        return buildable.buildBetaNode(primaryPlan, secondaryPlan, joinHelper.getPrimaryMask(),
//                joinHelper.getSecondaryMask(), joinHelper.getComplementerMask(), false);
//    }


    /**
     * Reduces the number of tuples by trimming (existentially quantifying) the set of variables that <ul>
     * <li> are visible in the subplan,
     * <li> are not exported parameters,
     * <li> have all their constraints already enforced in the subplan,
     * </ul> and thus will not be needed anymore.
     *
     * @param onlyIfNotDetermined if true, no trimming performed unless there is at least one variable that is not functionally determined
     * @return the plan after the trimming (possibly the original)
     * @since 1.5
     */
    public static SubPlan trimUnneccessaryVariables(SubPlanFactory planFactory, /*IOperationCompiler buildable,*/
            SubPlan plan, boolean onlyIfNotDetermined, QueryAnalyzer analyzer) {
        Set<PVariable> canBeTrimmed = new HashSet<PVariable>();
        Set<PVariable> variablesInPlan = plan.getVisibleVariables();
        for (PVariable trimCandidate : variablesInPlan) {
            if (trimCandidate.getReferringConstraintsOfType(ExportedParameter.class).isEmpty()) {
                if (plan.getAllEnforcedConstraints().containsAll(trimCandidate.getReferringConstraints()))
                    canBeTrimmed.add(trimCandidate);
            }
        }
        final Set<PVariable> retainedVars = setMinus(variablesInPlan, canBeTrimmed);
        if (!canBeTrimmed.isEmpty() && !(onlyIfNotDetermined && areVariablesDetermined(plan, retainedVars, canBeTrimmed, analyzer, false))) {
            // TODO add smart ordering?
            plan = planFactory.createSubPlan(new PProject(retainedVars), plan);
        }
        return plan;
    }

    /**
     * @return true iff a set of given variables functionally determine all visible variables in the subplan according to the subplan's constraints
     * @param strict if true, only "hard" dependencies are taken into account that are strictly enforced by the model representation;
     *  if false, user-provided soft dependencies are included as well, that are anticipated but not guaranteed by the storage mechanism;
     *  use true if superfluous dependencies may taint the correctness of a computation, false if they would merely impact performance
     * @since 1.5
     */
    public static boolean areAllVariablesDetermined(SubPlan plan, Collection<PVariable> determining, QueryAnalyzer analyzer, boolean strict) {
        return areVariablesDetermined(plan, determining, plan.getVisibleVariables(), analyzer, strict);
    }

    /**
     * @return true iff one set of given variables functionally determine the other set according to the subplan's constraints
     * @param strict if true, only "hard" dependencies are taken into account that are strictly enforced by the model representation;
     *  if false, user-provided soft dependencies are included as well, that are anticipated but not guaranteed by the storage mechanism;
     *  use true if superfluous dependencies may taint the correctness of a computation, false if they would merely impact performance
     * @since 1.5
     */
    public static boolean areVariablesDetermined(SubPlan plan, Collection<PVariable> determining, Collection<PVariable> determined,
            QueryAnalyzer analyzer, boolean strict) {
        Map<Set<PVariable>, Set<PVariable>> dependencies = analyzer.getFunctionalDependencies(plan.getAllEnforcedConstraints(), strict);
        final Set<PVariable> closure = FunctionalDependencyHelper.closureOf(determining, dependencies);
        final boolean isDetermined = closure.containsAll(determined);
        return isDetermined;
    }

    private static <T> Set<T> setMinus(Set<T> a, Set<T> b) {
        Set<T> difference = new HashSet<T>(a);
        difference.removeAll(b);
        return difference;
    }

    /**
     * Finds an arbitrary constraint that is not enforced at the given plan.
     *
     * @param pSystem
     * @param plan
     * @return a PConstraint that is not enforced, if any, or null if all are enforced
     */
    public static PConstraint getAnyUnenforcedConstraint(PBody pSystem,
            SubPlan plan) {
        Set<PConstraint> allEnforcedConstraints = plan.getAllEnforcedConstraints();
        Set<PConstraint> constraints = pSystem.getConstraints();
        for (PConstraint pConstraint : constraints) {
            if (!allEnforcedConstraints.contains(pConstraint))
                return pConstraint;
        }
        return null;
    }

    /**
     * Skips the last few steps, if any, that are projections, so that a custom projection can be added instead.
     * Useful for connecting body final plans into the production node.
     *
     * @since 2.1
     */
    public static SubPlan eliminateTrailingProjections(SubPlan plan) {
        while (plan.getOperation() instanceof PProject)
            plan = plan.getParentPlans().get(0);
        return plan;
    }

    /**
     * Verifies whether all constraints are enforced and exported parameters are present.
     *
     * @param pSystem
     * @param plan
     * @throws InterpreterRuntimeException
     */
    public static void finalCheck(final PBody pSystem, SubPlan plan, IQueryMetaContext context) {
        PConstraint unenforcedConstraint = getAnyUnenforcedConstraint(pSystem, plan);
        if (unenforcedConstraint != null) {
            throw new QueryProcessingException(
                    "Pattern matcher construction terminated without successfully enforcing constraint {1}."
                            + " Could be caused if the value of some variables can not be deduced, e.g. by circularity of pattern constraints.",
                    new String[] { unenforcedConstraint.toString() }, "Could not enforce a pattern constraint", null);
        }
        for (ExportedParameter export : pSystem
                .getConstraintsOfType(ExportedParameter.class)) {
            if (!export.isReadyAt(plan, context)) {
                throw new QueryProcessingException(
                        "Exported pattern parameter {1} could not be deduced during pattern matcher construction."
                                + " A pattern constraint is required to positively deduce its value.",
                        new String[] { export.getParameterName() }, "Could not calculate pattern parameter",
                        null);
            }
        }
    }

}
