/*******************************************************************************
 * Copyright (c) 2010-2014, Marton Bur, Akos Horvath, Zoltan Ujhelyi, Istvan Rath and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.interpreter.localsearch.planner;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import tools.refinery.interpreter.localsearch.planner.cost.ICostFunction;
import tools.refinery.interpreter.localsearch.planner.util.OperationCostComparator;
import tools.refinery.interpreter.localsearch.matcher.integration.LocalSearchHints;
import tools.refinery.interpreter.matchers.backend.ResultProviderRequestor;
import tools.refinery.interpreter.matchers.context.IQueryBackendContext;
import tools.refinery.interpreter.matchers.planning.SubPlan;
import tools.refinery.interpreter.matchers.planning.SubPlanFactory;
import tools.refinery.interpreter.matchers.planning.operations.PApply;
import tools.refinery.interpreter.matchers.planning.operations.PProject;
import tools.refinery.interpreter.matchers.planning.operations.PStart;
import tools.refinery.interpreter.matchers.psystem.PBody;
import tools.refinery.interpreter.matchers.psystem.PConstraint;
import tools.refinery.interpreter.matchers.psystem.PVariable;
import tools.refinery.interpreter.matchers.util.Sets;

/**
 * This class contains the logic for local search plan calculation based on costs of the operations.
 * Its name refers to the fact that the strategy tries to use as much information as available about
 * the model on which the matching is initiated. When no runtime info is available, it falls back to
 * the information available from the metamodel durint operation cost calculation.
 *
 * The implementation is based on the paper "Gergely Varró, Frederik Deckwerth, Martin Wieber, and Andy Schürr:
 * An algorithm for generating model-sensitive search plans for pattern matching on EMF models"
 * (DOI: 10.1007/s10270-013-0372-2)
 *
 * @author Marton Bur
 * @noreference This class is not intended to be referenced by clients.
 */
public class LocalSearchRuntimeBasedStrategy {

    private final OperationCostComparator infoComparator = new OperationCostComparator();


    /**
     * Converts a plan to the standard format
     */
    protected SubPlan convertPlan(Set<PVariable> initialBoundVariables, PlanState searchPlan) {
        PBody pBody;
        pBody = searchPlan.getAssociatedPBody();

        // Create a starting plan
        SubPlanFactory subPlanFactory = new SubPlanFactory(pBody);

        // We assume that the adornment (now the bound variables) is previously set
        SubPlan plan = subPlanFactory.createSubPlan(new PStart(initialBoundVariables));

        List<PConstraintInfo> operations = searchPlan.getOperations();
        for (PConstraintInfo pConstraintPlanInfo : operations) {
            PConstraint pConstraint = pConstraintPlanInfo.getConstraint();
            plan = subPlanFactory.createSubPlan(new PApply(pConstraint), plan);
        }

        return subPlanFactory.createSubPlan(new PProject(pBody.getSymbolicParameterVariables()), plan);
    }

    /**
     * The implementation of a local search-based algorithm to create a search plan for a flattened (and normalized)
     * PBody
     * @param pBody for which the plan is to be created
     * @param initialBoundVariables variables that are known to have already assigned values
     * @param context the backend context
     * @param resultProviderRequestor requestor for accessing result providers of called patterns
     * @param configuration the planner configuration
     * @return the complete search plan for the given {@link PBody}
     * @since 2.1
     */
    protected PlanState plan(PBody pBody, Set<PVariable> initialBoundVariables,
            IQueryBackendContext context, final ResultProviderRequestor resultProviderRequestor,
            LocalSearchHints configuration) {
        final ICostFunction costFunction = configuration.getCostFunction();
        PConstraintInfoInferrer pConstraintInfoInferrer = new PConstraintInfoInferrer(
                configuration.isUseBase(), context, resultProviderRequestor, costFunction::apply);

        // Create mask infos
        Set<PConstraint> constraintSet = pBody.getConstraints();
        List<PConstraintInfo> constraintInfos =
                pConstraintInfoInferrer.createPConstraintInfos(constraintSet);

        // Calculate the characteristic function
        // The characteristic function tells whether a given adornment is backward reachable from the (B)* state, where
        // each variable is bound.
        // The characteristic function is represented as a set of set of variables
        // TODO this calculation is not not implemented yet, thus the contents of the returned set is not considered later
        List<Set<PVariable>> reachableBoundVariableSets = reachabilityAnalysis(pBody, constraintInfos);
        int k = configuration.getRowCount();
        PlanState searchPlan = calculateSearchPlan(pBody, initialBoundVariables, k, reachableBoundVariableSets, constraintInfos);
        return searchPlan;
    }

    private PlanState calculateSearchPlan(PBody pBody, Set<PVariable> initialBoundVariables, int k,
            List<Set<PVariable>> reachableBoundVariableSets, List<PConstraintInfo> allMaskInfos) {

        List<PConstraintInfo> allPotentialExtendInfos = new ArrayList<>();
        List<PConstraintInfo> allPotentialCheckInfos = new ArrayList<>();
        Map<PVariable, List<PConstraintInfo>> checkOpsByVariables = new HashMap<>();
        Map<PVariable, Collection<PConstraintInfo>> extendOpsByBoundVariables = new HashMap<>();

        for (PConstraintInfo op : allMaskInfos) {
            if (op.getFreeVariables().isEmpty()) { // CHECK
                allPotentialCheckInfos.add(op);
            } else { // EXTEND
                allPotentialExtendInfos.add(op);
                for (PVariable variable : op.getBoundVariables()) {
                    extendOpsByBoundVariables.computeIfAbsent(variable, v -> new ArrayList<>()).add(op);
                }
            }
        }
        // For CHECKs only, we must start from lists that are ordered by the cost of the constraint application
        Collections.sort(allPotentialCheckInfos, infoComparator);  // costs are eagerly needed for check ops
        for (PConstraintInfo op : allPotentialCheckInfos) {
            for (PVariable variable : op.getBoundVariables()) {
                checkOpsByVariables.computeIfAbsent(variable, v -> new ArrayList<>()).add(op);
            }
        }
        // costs are not needed for extend ops until they are first applied (TODO make cost computaiton on demand)


        // rename for better understanding
        Set<PVariable> boundVariables = initialBoundVariables;
        Set<PVariable> freeVariables = Sets.difference(pBody.getUniqueVariables(), initialBoundVariables);

        int variableCount = pBody.getUniqueVariables().size();
        int n = freeVariables.size();

        List<List<PlanState>> stateTable = initializeStateTable(k, n);

        // Set initial state: begin with an empty operation list
        PlanState initialState = new PlanState(pBody, boundVariables);

        // Initial state creation, categorizes all operations; add present checks to operationsList
        initialState.updateExtends(allPotentialExtendInfos);
        initialState.applyChecks(allPotentialCheckInfos);
        stateTable.get(n).add(0, initialState);

        // stateTable.get(0) will contain the states with adornment B*
        for (int i = n; i > 0; i--) {
            for (int j = 0; j < k && j < stateTable.get(i).size(); j++) {
                PlanState currentState = stateTable.get(i).get(j);

                for (PConstraintInfo constraintInfo : currentState.getPresentExtends()) {
                    // for each present EXTEND operation
                    PlanState newState = calculateNextState(currentState, constraintInfo);
                    // also eagerly perform any CHECK operations that become applicable (extends still deferred)
                    newState.applyChecksBasedOnDelta(checkOpsByVariables);

                    if(currentState.getBoundVariables().size() == newState.getBoundVariables().size()){
                        // This means no variable binding was done, go on with the next constraint info
                        continue;
                    }
                    int i2 = variableCount - newState.getBoundVariables().size();

                    List<Integer> newIndices = determineIndices(stateTable, i2, newState, k);
                    int a = newIndices.get(0);
                    int c = newIndices.get(1);

                    if (checkInsertCondition(stateTable.get(i2), newState, reachableBoundVariableSets, a, c, k)) {
                        updateExtends(newState, currentState, extendOpsByBoundVariables); // preprocess next steps
                        insert(stateTable,i2, newState, a, c, k);
                    }
                }
            }
        }

        return stateTable.get(0).get(0);
    }

    private List<List<PlanState>> initializeStateTable(int k, int n) {
        List<List<PlanState>> stateTable = new ArrayList<>();
        // Initialize state table and fill it with null
        for (int i = 0; i <= n ; i++) {
            stateTable.add(new ArrayList<>());
        }
        return stateTable;
    }

    private void insert(List<List<PlanState>> stateTable, int idx, PlanState newState, int a, int c, int k) {
        stateTable.get(idx).add(c, newState);
        while(stateTable.get(idx).size() > k){
            // Truncate back to size k when grows too big
            stateTable.set(idx, stateTable.get(idx).subList(0, k));
        }
    }

    private void updateExtends(PlanState newState, PlanState currentState,
            Map<PVariable, ? extends Collection<PConstraintInfo>> extendOpsByBoundVariables)
    {
        List<PConstraintInfo> presentExtends = currentState.getPresentExtends();

        // Recategorize operations
        newState.updateExtendsBasedOnDelta(presentExtends, extendOpsByBoundVariables);

        return;
    }

    private boolean checkInsertCondition(List<PlanState> list, PlanState newState,
            List<Set<PVariable>> reachableBoundVariableSets, int a, int c, int k) {
//        boolean isAmongBestK = (a == (k + 1)) && c < a && reachableBoundVariableSets.contains(newState.getBoundVariables());
        boolean isAmongBestK = a == k && c < a ;
        boolean isBetterThanCurrent = a < k && c <= a;

        return isAmongBestK || isBetterThanCurrent;
    }

    private List<Integer> determineIndices(List<List<PlanState>> stateTable, int i2, PlanState newState, int k) {
        int a = k;
        int c = 0;
        List<Integer> acIndices = new ArrayList<>();
        for (int j = 0; j < k; j++) {
            if (j < stateTable.get(i2).size()) {
                PlanState stateInTable = stateTable.get(i2).get(j);
                if (newState.getBoundVariables().equals(stateInTable.getBoundVariables())) {
                    // The new state has the same adornment as the stored one - they are not adornment disjoint
                    a = j;
                }
                if (newState.getCost() >= stateInTable.getCost()) {
                    c = j + 1;
                }
            } else {
                break;
            }
        }

        acIndices.add(a);
        acIndices.add(c);
        return acIndices;
    }

    private PlanState calculateNextState(PlanState currentState, PConstraintInfo constraintInfo) {
        return currentState.cloneWithApplied(constraintInfo);
    }

    private List<Set<PVariable>> reachabilityAnalysis(PBody pBody, List<PConstraintInfo> constraintInfos) {
        // TODO implement reachability analisys, also save/persist the results somewhere
        List<Set<PVariable>> reachableBoundVariableSets = new ArrayList<>();
        return reachableBoundVariableSets;
    }


}
