/*******************************************************************************
 * Copyright (c) 2004-2008 Gabor Bergmann and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package tools.refinery.interpreter.matchers.planning;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.stream.Collectors;

import tools.refinery.interpreter.matchers.planning.helpers.TypeHelper;
import tools.refinery.interpreter.matchers.planning.operations.POperation;
import tools.refinery.interpreter.matchers.planning.operations.PProject;
import tools.refinery.interpreter.matchers.planning.operations.PStart;
import tools.refinery.interpreter.matchers.context.IQueryMetaContext;
import tools.refinery.interpreter.matchers.psystem.PBody;
import tools.refinery.interpreter.matchers.psystem.PConstraint;
import tools.refinery.interpreter.matchers.psystem.PVariable;
import tools.refinery.interpreter.matchers.psystem.TypeJudgement;

/**
 * A plan representing a subset of (or possibly all the) constraints evaluated. A SubPlan instance is responsible for
 * representing a state of the plan; but after it is initialized it is expected be immutable
 *  (exception: inferred constraints, see {@link #inferConstraint(PConstraint)}).
 *
 * <p> A SubPlan is constructed by applying a {@link POperation} on zero or more parent SubPlans.
 * Important maintained information: <ul>
 * <li>set of <b>variables</b> whose values are known when the runtime evaluation is at this stage,
 * <li>set of <b>constraints</b> that are known to hold true at this point.
 * </ul>
 *
 * <p> Recommended to instantiate via a {@link SubPlanFactory} or subclasses,
 * so that query planners can subclass SubPlan if needed.
 *
 * @author Gabor Bergmann
 *
 */
public class SubPlan {
    private PBody body;
    private List<? extends SubPlan> parentPlans;
    private POperation operation;

    private final Set<PVariable> visibleVariables;
    private final Set<PVariable> allVariables;
    private final Set<PVariable> introducedVariables; // delta compared to first parent
    private Set<PConstraint> allConstraints;
    private Set<PConstraint> deltaConstraints; // delta compared to all parents
    private Set<PConstraint> externallyInferredConstraints; // inferred in addition to direct consequences of the operation and parents





    /**
     * A SubPlan is constructed by applying a {@link POperation} on zero or more parent SubPlans.
     */
    public SubPlan(PBody body, POperation operation, SubPlan... parentPlans) {
        this(body, operation, Arrays.asList(parentPlans));
    }
    /**
     * A SubPlan is constructed by applying a {@link POperation} on zero or more parent SubPlans.
     */
    public SubPlan(PBody body, POperation operation, List<? extends SubPlan> parentPlans) {
        super();
        this.body = body;
        this.parentPlans = parentPlans;
        this.operation = operation;

        this.externallyInferredConstraints = new HashSet<PConstraint>();
        this.deltaConstraints = new HashSet<PConstraint>(operation.getDeltaConstraints());

        this.allVariables = new HashSet<PVariable>();
        for (PConstraint constraint: deltaConstraints) {
            this.allVariables.addAll(constraint.getDeducedVariables());
        }
        this.allConstraints = new HashSet<PConstraint>(deltaConstraints);
        for (SubPlan parentPlan: parentPlans) {
            this.allConstraints.addAll(parentPlan.getAllEnforcedConstraints());
            this.allVariables.addAll(parentPlan.getAllDeducedVariables());
        }

        // TODO this is ugly a bit
        if (operation instanceof PStart) {
            this.visibleVariables = new HashSet<PVariable>(((PStart) operation).getAPrioriVariables());
            this.allVariables.addAll(visibleVariables);
        } else if (operation instanceof PProject) {
            this.visibleVariables = new HashSet<PVariable>(((PProject) operation).getToVariables());
        } else {
            this.visibleVariables = new HashSet<PVariable>();
            for (SubPlan parentPlan: parentPlans)
                this.visibleVariables.addAll(parentPlan.getVisibleVariables());
            for (PConstraint constraint: deltaConstraints)
                this.visibleVariables.addAll(constraint.getDeducedVariables());
        }

        this.introducedVariables = new HashSet<PVariable>(this.visibleVariables);
        if (!parentPlans.isEmpty())
            introducedVariables.removeAll(parentPlans.get(0).getVisibleVariables());

        operation.checkConsistency(this);
    }


    @Override
    public String toString() {
        return toLongString();
    }
    public String toShortString() {
        return String.format("Plan{%s}:%s",
                visibleVariables.stream().map(PVariable::getName).collect(Collectors.joining(",")),
                operation.getShortName());
    }
    public String toLongString() {
        return String.format("%s<%s>",
                toShortString(),
                parentPlans.stream().map(Object::toString).collect(Collectors.joining("; ")));
    }


    /**
     * All constraints that are known to hold at this point
     */
    public Set<PConstraint> getAllEnforcedConstraints() {
        return allConstraints;
    }

    /**
     * The new constraints enforced at this stage of plan, that aren't yet enforced at parents
     * (results are also included in {@link SubPlan#getAllEnforcedConstraints()})
     */
    public Set<PConstraint> getDeltaEnforcedConstraints() {
        return deltaConstraints;
    }

    /**
     * Indicate that a given constraint was found to be automatically satisfied at this point
     *   without additional operations.
     * (results are also included in {@link SubPlan#getDeltaEnforcedConstraints()})
     *
     * <p>Warning: not propagated automatically to child plans,
     *  so best to invoke before constructing further SubPlans. </p>
     */
    public void inferConstraint(PConstraint constraint) {
        externallyInferredConstraints.add(constraint);
        deltaConstraints.add(constraint);
        allConstraints.add(constraint);
    }

    public PBody getBody() {
        return body;
    }

    /**
     * Variables which are assigned a value at this point
     * (results are also included in {@link SubPlan#getAllDeducedVariables()})
     */
    public Set<PVariable> getVisibleVariables() {
        return visibleVariables;
    }
    /**
     * Variables which have been assigned a value;
     * includes visible variables (see {@link #getVisibleVariables()})
     *   and additionally any variables hidden by a projection (see {@link PProject}).
     */
    public Set<PVariable> getAllDeducedVariables() {
        return allVariables;
    }
    /**
     * Delta compared to first parent: variables that are visible here but were not visible at the first parent.
     */
    public Set<PVariable> getIntroducedVariables() {
        return introducedVariables;
    }
    public List<? extends SubPlan> getParentPlans() {
        return parentPlans;
    }
    public POperation getOperation() {
        return operation;
    }


    /**
     * The closure of all type judgments of enforced constraints at this point.
     * <p> No subsumption applied.
     */
    public Set<TypeJudgement> getAllImpliedTypeJudgements(IQueryMetaContext context) {
        Set<TypeJudgement> impliedJudgements = allImpliedTypeJudgements.get(context);
        if (impliedJudgements == null) {
            Set<TypeJudgement> equivalentJudgements = TypeHelper.getDirectJudgements(getAllEnforcedConstraints(), context);
            impliedJudgements = TypeHelper.typeClosure(equivalentJudgements, context);

            allImpliedTypeJudgements.put(context, impliedJudgements);
        }
        return impliedJudgements;
    }
    private WeakHashMap<IQueryMetaContext, Set<TypeJudgement>> allImpliedTypeJudgements = new WeakHashMap<IQueryMetaContext, Set<TypeJudgement>>();


    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result
                + ((operation == null) ? 0 : operation.hashCode());
        result = prime * result
                + ((parentPlans == null) ? 0 : parentPlans.hashCode());
        return result;
    }
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (!(obj instanceof SubPlan))
            return false;
        SubPlan other = (SubPlan) obj;
        if (operation == null) {
            if (other.operation != null)
                return false;
        } else if (!operation.equals(other.operation))
            return false;
        if (parentPlans == null) {
            if (other.parentPlans != null)
                return false;
        } else if (!parentPlans.equals(other.parentPlans))
            return false;
        return true;
    }


}
