/*******************************************************************************
 * Copyright (c) 2010-2014, Marton Bur, Daniel Segesdi, Akos Horvath, Zoltan Ujhelyi, Istvan Rath and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.interpreter.localsearch.planner.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import tools.refinery.interpreter.matchers.planning.SubPlan;
import tools.refinery.interpreter.matchers.planning.operations.PApply;
import tools.refinery.interpreter.matchers.planning.operations.POperation;
import tools.refinery.interpreter.matchers.psystem.PConstraint;
import tools.refinery.interpreter.matchers.psystem.PVariable;
import tools.refinery.interpreter.matchers.psystem.basicdeferred.ExportedParameter;
import tools.refinery.interpreter.matchers.psystem.queries.PParameter;

/**
 *
 * Helper methods for compiling SubPlans
 *
 * @author Marton Bur
 *
 */
public class CompilerHelper {

    private CompilerHelper() {/*Utility class constructor*/}

    private static boolean isUserSpecified(PVariable var) {
        return !var.isVirtual() && !var.getName().startsWith("_");
    }

    public static Map<PVariable, Integer> createVariableMapping(SubPlan plan) {
        Map<PVariable, Integer> variableMapping = new HashMap<>();

        int variableNumber = 0;

        // Important note: this list might contain duplications when parameters are made equal inside the pattern
        // This is the expected and normal behavior
        List<PVariable> symbolicParameterVariables = plan.getBody().getSymbolicParameterVariables();
        for (PVariable pVariable : symbolicParameterVariables) {
            if (!variableMapping.containsKey(pVariable)) {
                variableMapping.put(pVariable, variableNumber++);
            }
        }

        List<PVariable> allVariables = new ArrayList<>(plan.getBody().getUniqueVariables());
        Collections.sort(allVariables, (left, right) -> {
                    boolean leftUserSpecified = isUserSpecified(left);
                    boolean rightUserSpecified = isUserSpecified(right);
                    if (leftUserSpecified && !rightUserSpecified) {
                        return -1;
                    } else if (!leftUserSpecified && rightUserSpecified) {
                        return +1;
                    } else {
                        return left.getName().compareTo(right.getName());
                    }
                });
        for (PVariable pVariable : allVariables) {
            if (!variableMapping.containsKey(pVariable)) {
                variableMapping.put(pVariable, variableNumber++);
            }
        }

        return variableMapping;
    }

    public static Map<PConstraint, Set<Integer>> cacheVariableBindings(SubPlan plan,
            Map<PVariable, Integer> variableMappings, Set<PParameter> adornment) {

        Set<Integer> externallyBoundVariables = getVariableIndicesForParameters(plan, variableMappings,
                adornment);

        Map<PConstraint, Set<Integer>> variableBindings = new HashMap<>();

        List<SubPlan> allPlansInHierarchy = getAllParentPlans(plan);
        for (SubPlan subPlan : allPlansInHierarchy) {
            POperation operation = subPlan.getOperation();

            if (operation instanceof PApply) {
                PConstraint pConstraint = ((PApply) operation).getPConstraint();
                Set<Integer> boundVariableIndices = getParametersBoundByParentPlan(variableMappings, subPlan);
                boundVariableIndices.addAll(externallyBoundVariables);

                variableBindings.put(pConstraint, boundVariableIndices);
            }
        }
        return variableBindings;
    }

    /**
     * Returns the list of variable indexes that are bound by the parent plan.
     */
    private static Set<Integer> getParametersBoundByParentPlan(Map<PVariable, Integer> variableMappings,
            SubPlan subPlan) {
        if (!subPlan.getParentPlans().isEmpty()) {
            SubPlan parentPlan = subPlan.getParentPlans().get(0);
            Set<PConstraint> enforcedConstraints = parentPlan.getAllEnforcedConstraints();
            Set<PVariable> affectedVariables = getAffectedVariables(enforcedConstraints);
            return getVariableIndices(variableMappings, affectedVariables);
        }
        return Collections.emptySet();
    }

    /**
     * @param plan
     * @return all the ancestor plans including the given plan
     */
    private static List<SubPlan> getAllParentPlans(SubPlan plan) {
        SubPlan currentPlan = plan;
        List<SubPlan> allPlans = new ArrayList<>();
        allPlans.add(plan);
        while (!currentPlan.getParentPlans().isEmpty()) {
            // In the local search it is assumed that only a single parent exists
            currentPlan = currentPlan.getParentPlans().get(0);
            allPlans.add(currentPlan);
        }

        return allPlans;
    }

    /**
     * @param variableMappings
     *            the mapping between variables and their indices
     * @param variables
     *            variables to get the indices for
     * @return the set of variable indices for the given variables
     */
    private static Set<Integer> getVariableIndices(Map<PVariable, Integer> variableMappings,
            Iterable<PVariable> variables) {
        Set<Integer> variableIndices = new HashSet<>();
        for (PVariable pVariable : variables) {
            variableIndices.add(variableMappings.get(pVariable));
        }
        return variableIndices;
    }

    /**
     * Returns all affected variables of the given PConstraints.
     */
    private static Set<PVariable> getAffectedVariables(Set<PConstraint> pConstraints) {
        Set<PVariable> allDeducedVariables = new HashSet<>();
        for (PConstraint pConstraint : pConstraints) {
            allDeducedVariables.addAll(pConstraint.getAffectedVariables());
        }
        return allDeducedVariables;
    }

    /**
     * Transforms the index of a parameter into the index of a variable of the normalized body.
     *
     * @param plan
     *            the SubPlan containing the original body and its parameters
     * @param variableMappings
     *            the mapping of PVariables to their indices
     * @param parameters
     *            a set of parameters
     * @return the index of the variable corresponding to the parameter at the given index
     */
    private static Set<Integer> getVariableIndicesForParameters(SubPlan plan,
            Map<PVariable, Integer> variableMappings, Set<PParameter> parameters) {
        Map<PParameter, PVariable> parameterMapping = new HashMap<>();
        for (ExportedParameter constraint : plan.getBody().getSymbolicParameters()) {
            parameterMapping.put(constraint.getPatternParameter(), constraint.getParameterVariable());
        }

        Set<Integer> variableIndices = new HashSet<>();
        for (PParameter parameter : parameters) {
            PVariable parameterVariable = parameterMapping.get(parameter);
            if (parameterVariable == null) {
                // XXX In case of older (pre-1.4) VIATRA versions, PParameters were not stable, see bug 498348
                parameterVariable = plan.getBody().getVariableByNameChecked(parameter.getName());
            }
            Integer variableIndex = variableMappings.get(parameterVariable);
            variableIndices.add(variableIndex);
        }
        return variableIndices;
    }

    /**
     * Extracts the operations from a SubPlan into a list of POperations in the order of execution
     *
     * @param plan
     *            the SubPlan from wich the POperations should be extracted
     * @return list of POperations extracted from the <code>plan</code>
     */
    public static List<POperation> createOperationsList(SubPlan plan) {
        List<POperation> operationsList = new ArrayList<>();
        while (plan.getParentPlans().size() > 0) {
            operationsList.add(plan.getOperation());
            SubPlan parentPlan = plan.getParentPlans().get(0);
            plan = parentPlan;
        }
        operationsList.add(plan.getOperation());

        Collections.reverse(operationsList);
        return operationsList;
    }

}
