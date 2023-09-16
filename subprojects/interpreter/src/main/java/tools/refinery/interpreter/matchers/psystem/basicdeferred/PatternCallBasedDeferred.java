/*******************************************************************************
 * Copyright (c) 2004-2010 Gabor Bergmann and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package tools.refinery.interpreter.matchers.psystem.basicdeferred;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import tools.refinery.interpreter.matchers.planning.QueryProcessingException;
import tools.refinery.interpreter.matchers.psystem.IQueryReference;
import tools.refinery.interpreter.matchers.psystem.PBody;
import tools.refinery.interpreter.matchers.psystem.PConstraint;
import tools.refinery.interpreter.matchers.psystem.PVariable;
import tools.refinery.interpreter.matchers.psystem.VariableDeferredPConstraint;
import tools.refinery.interpreter.matchers.psystem.queries.PQuery;
import tools.refinery.interpreter.matchers.tuple.Tuple;

/**
 * @author Gabor Bergmann
 *
 */
public abstract class PatternCallBasedDeferred extends VariableDeferredPConstraint implements IQueryReference {

    protected Tuple actualParametersTuple;

    protected abstract void doDoReplaceVariables(PVariable obsolete, PVariable replacement);

    protected abstract Set<PVariable> getCandidateQuantifiedVariables();

    protected PQuery query;
    private Set<PVariable> deferringVariables;

    public PatternCallBasedDeferred(PBody pBody, Tuple actualParametersTuple,
            PQuery pattern, Set<PVariable> additionalAffectedVariables) {
        super(pBody, union(actualParametersTuple.<PVariable> getDistinctElements(), additionalAffectedVariables));
        this.actualParametersTuple = actualParametersTuple;
        this.query = pattern;
    }

    public PatternCallBasedDeferred(PBody pBody, Tuple actualParametersTuple,
            PQuery pattern) {
        this(pBody, actualParametersTuple, pattern, Collections.<PVariable> emptySet());
    }

    private static Set<PVariable> union(Set<PVariable> a, Set<PVariable> b) {
        Set<PVariable> result = new HashSet<PVariable>();
        result.addAll(a);
        result.addAll(b);
        return result;
    }

    @Override
    public Set<PVariable> getDeferringVariables() {
        if (deferringVariables == null) {
            deferringVariables = new HashSet<PVariable>();
            for (PVariable var : getCandidateQuantifiedVariables()) {
                if (var.isDeducable())
                    deferringVariables.add(var);
            }
        }
        return deferringVariables;
    }

    @Override
    public void checkSanity() {
        super.checkSanity();
        for (Object obj : this.actualParametersTuple.getDistinctElements()) {
            PVariable var = (PVariable) obj;
            if (!getDeferringVariables().contains(var)) {
                // so this is a free variable of the NAC / aggregation?
                for (PConstraint pConstraint : var.getReferringConstraints()) {
                    if (pConstraint != this
                            && !(pConstraint instanceof Equality && ((Equality) pConstraint).isMoot()))
                        throw new QueryProcessingException(
                                "Variable {1} of constraint {2} is not a positively determined part of the pattern, yet it is also affected by {3}.",
                                new String[] { var.toString(), this.toString(), pConstraint.toString() },
                                "Read-only variable can not be deduced", null);
                }
            }
        }

    }

//    public SubPlan getSidePlan(IOperationCompiler compiler) throws QueryPlannerException {
//        SubPlan sidePlan = compiler.patternCallPlan(actualParametersTuple, query);
//        sidePlan = BuildHelper.enforceVariableCoincidences(compiler, sidePlan);
//        return sidePlan;
//    }

    @Override
    protected void doReplaceVariable(PVariable obsolete, PVariable replacement) {
        if (deferringVariables != null) {
            // FAIL instead of hopeless attempt to fix
            // if (deferringVariables.remove(obsolete)) deferringVariables.add(replacement);
            throw new IllegalStateException("Cannot replace variables on " + this
                    + " when deferring variables have already been identified.");
        }
        actualParametersTuple = actualParametersTuple.replaceAll(obsolete, replacement);
        doDoReplaceVariables(obsolete, replacement);
    }

    public Tuple getActualParametersTuple() {
        return actualParametersTuple;
    }

    @Override
    public PQuery getReferredQuery() {
        return query;
    }

}
