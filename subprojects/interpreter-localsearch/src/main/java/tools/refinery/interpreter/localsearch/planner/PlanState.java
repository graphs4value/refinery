/*******************************************************************************
 * Copyright (c) 2010-2015, Marton Bur, Zoltan Ujhelyi, Akos Horvath, Istvan Rath and Daniel Varro
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
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import tools.refinery.interpreter.localsearch.planner.util.OperationCostComparator;
import tools.refinery.interpreter.matchers.algorithms.OrderedIterableMerge;
import tools.refinery.interpreter.matchers.psystem.PBody;
import tools.refinery.interpreter.matchers.psystem.PConstraint;
import tools.refinery.interpreter.matchers.psystem.PVariable;

/**
 * This class represents the state of the plan during planning.
 *
 * <p> A PlanState represents a sequence of operations (operationsList) and caches the computed cost
 * for this operation sequence. The list and the cost are initialized in the constructor.
 * However, #categorizeChecks() also updates the operations list (by suffixing checks)
 *
 * @author Marton Bur
 * @noreference This class is not intended to be referenced by clients.
 */
public class PlanState {

    private final PBody pBody;
    private final List<PConstraintInfo> operationsList;
    private final Set<PVariable> boundVariables;
    private final Collection<PVariable> deltaVariables; /* bound since ancestor plan */
    private final Set<PConstraint> enforcedConstraints;

    private double cummulativeProduct;
    private double cost;

    private static Comparator<PConstraintInfo> infoComparator = new OperationCostComparator();

    /*
     * For a short explanation of past, present and future operations,
     * see class
     */
    private List<PConstraintInfo> presentExtends;

    /**
     * Creates an initial state
     */
    public PlanState(PBody pBody, Set<PVariable> boundVariables) {

        this(pBody, new ArrayList<>(), boundVariables, boundVariables /* also the delta */,
                0.0 /* initial cost */, 1.0 /*initial branch count */);
    }

    public PlanState cloneWithApplied(PConstraintInfo op) {
        // Create operation list based on the current state
        ArrayList<PConstraintInfo> newOperationsList =
                // pre-reserve excess capacity for later addition of CHECK ops
                new ArrayList<>(pBody.getConstraints().size());
        newOperationsList.addAll(this.getOperations());
        newOperationsList.add(op);

        // Bind the variables of the op
        Collection<PVariable> deltaVariables = op.getFreeVariables();
        Set<PVariable> allBoundVariables =
                // pre-reserve exact capacity as variables are known
                // (will not be affected by adding CHECK ops later)
                new HashSet<>(this.getBoundVariables().size() + deltaVariables.size());
        allBoundVariables.addAll(this.getBoundVariables());
        allBoundVariables.addAll(deltaVariables);

        PlanState newState = new PlanState(getAssociatedPBody(), newOperationsList, allBoundVariables, deltaVariables,
                cost, cummulativeProduct);
        newState.accountNewOperation(op);
        return newState;
    }

    private PlanState(PBody pBody, List<PConstraintInfo> operationsList,
            Set<PVariable> boundVariables, Collection<PVariable> deltaVariables,
            double cost, double cummulativeProduct)
    {
        this.pBody = pBody;
        this.operationsList = operationsList;
        this.boundVariables = boundVariables;
        this.enforcedConstraints = new HashSet<>();
        this.deltaVariables = deltaVariables;
        this.cost = cost;
        this.cummulativeProduct = cummulativeProduct;
    }

    // NOT included for EXTEND: bind all variables of op
    private void accountNewOperation(PConstraintInfo constraintInfo) {
        this.enforcedConstraints.add(constraintInfo.getConstraint());
        accountCost(constraintInfo);
    }

    private void accountCost(PConstraintInfo constraintInfo) {
        double constraintCost = constraintInfo.getCost();
        double branchFactor = constraintCost;
        if (constraintCost > 0){
            cost += cummulativeProduct * constraintCost;
            cummulativeProduct *= branchFactor;
        }
    }


    public Set<PConstraint> getEnforcedConstraints() {
        return enforcedConstraints;
    }

    /**
     * Re-categorizes given extend operations into already applied or no longer applicable ones (discarded),
     * immediately applicable ones (saved as presently viable extends),
     * and not yet applicable ones (discarded).
     *
     * @param allPotentialExtendInfos all other extends that may be applicable
     * to this plan state now or in the future;
     * MUST consist of "extend" constraint applications only (at least one free variable)
     */
    public void updateExtends(Iterable<PConstraintInfo> allPotentialExtendInfos) {
        presentExtends = new ArrayList<>();


        // categorize future/present extend constraint infos
        for (PConstraintInfo op : allPotentialExtendInfos) {
            updateExtendInternal(op);
        }
    }

    /**
     * Re-categorizes given extend operations into already applied or no longer applicable ones (discarded),
     * immediately applicable ones (saved as presently viable extends),
     * and not yet applicable ones (discarded).
     *
     * @param extendOpsByBoundVariables all EXTEND operations indexed by affected <i>bound</i> variables
     * MUST consist of "extend" constraint applications only (at least one free variable)
     */
    public void updateExtendsBasedOnDelta(
            Iterable<PConstraintInfo> previousPresentExtends,
            Map<PVariable, ? extends Collection<PConstraintInfo>> extendOpsByBoundVariables)
    {
        presentExtends = new ArrayList<>();
        if (operationsList.isEmpty())
            throw new IllegalStateException("Not applicable as starting step");

        for (PConstraintInfo extend: previousPresentExtends) {
            updateExtendInternal(extend);
        }

        Set<PConstraintInfo> affectedExtends = new HashSet<>();
        for (PVariable variable : deltaVariables) {
            // only those check ops may become applicable that have an affected variable in the delta
            Collection<PConstraintInfo> extendsForVariable = extendOpsByBoundVariables.get(variable);
            if (null != extendsForVariable) {
                affectedExtends.addAll(extendsForVariable);
            }
        }
        for (PConstraintInfo extend: affectedExtends) {
            updateExtendInternal(extend);
        }
    }

    private void updateExtendInternal(PConstraintInfo op) {
        if(!enforcedConstraints.contains(op.getConstraint())) {
            categorizeExtend(op);
        }
    }

    /**
     * Check operations that newly became applicable (see {@link #getDeltaVariables()})
     * are appended to operations lists.
     *
     * <p> Will never discover degenerate checks (of PConstraints with zero variables),
     * so must not use on initial state.
     *
     * @param allPotentialCheckInfos all CHECK operations
     * MUST consist of "check" constraint applications only (no free variables)
     * and must be iterable in decreasing order of cost
     *
     *
     */
    public void applyChecks(List<PConstraintInfo> allPotentialCheckInfos) {
        applyChecksInternal(allPotentialCheckInfos);
    }

    /**
     * Immediately applicable checks are appended to operations lists.
     *
     * @param checkOpsByVariables all CHECK operations indexed by affected variables
     * MUST consist of "check" constraint applications only (no free variables)
     * and each bucket must be iterable in decreasing order of cost
     */
    public void applyChecksBasedOnDelta(Map<PVariable, List<PConstraintInfo>> checkOpsByVariables) {
        if (operationsList.isEmpty())
            throw new IllegalStateException("Not applicable as starting step");

        Iterable<PConstraintInfo> affectedChecks = Collections.emptyList();

        for (PVariable variable : deltaVariables) {
            // only those check ops may become applicable that have an affected variable in the delta
            List<PConstraintInfo> checksForVariable = checkOpsByVariables.get(variable);
            if (null != checksForVariable) {
                affectedChecks = OrderedIterableMerge.mergeUniques(affectedChecks, checksForVariable, infoComparator);
            }
        }

        // checks retain their order, no re-sorting needed
        applyChecksInternal(affectedChecks);
    }

    private void applyChecksInternal(Iterable<PConstraintInfo> checks) {
        for (PConstraintInfo checkInfo : checks) {
            if (this.boundVariables.containsAll(checkInfo.getBoundVariables()) &&
                    !enforcedConstraints.contains(checkInfo.getConstraint()))
            {
                operationsList.add(checkInfo);
                accountNewOperation(checkInfo);
            }
        }
    }


    private void categorizeExtend(PConstraintInfo constraintInfo) {
        PConstraintCategory category = constraintInfo.getCategory(pBody, boundVariables);
        if (category == PConstraintCategory.PRESENT) {
            presentExtends.add(constraintInfo);
        } else {
            // do not categorize past/future operations
        }
    }


    public PBody getAssociatedPBody() {
        return pBody;
    }

    public List<PConstraintInfo> getOperations() {
        return operationsList;
    }

    public Set<PVariable> getBoundVariables() {
        return boundVariables;
    }

    /**
     * @return the derived cost of the plan contained in the state
     */
    public double getCost() {
        return cost;
    }


    /**
     * @return cumulative branching factor
     * @since 2.1
     */
    public double getCummulativeProduct() {
        return cummulativeProduct;
    }

    public List<PConstraintInfo> getPresentExtends() {
        return presentExtends;
    }

    /**
     * Contains only those variables that are added by the newest extend
     * (or the initially bound ones if no extend yet)
     */
    public Collection<PVariable> getDeltaVariables() {
        return deltaVariables;
    }


}
