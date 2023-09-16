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
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import tools.refinery.interpreter.matchers.planning.SubPlan;
import tools.refinery.interpreter.matchers.context.IQueryMetaContext;
import tools.refinery.interpreter.matchers.psystem.PBody;
import tools.refinery.interpreter.matchers.psystem.PVariable;
import tools.refinery.interpreter.matchers.psystem.TypeJudgement;
import tools.refinery.interpreter.matchers.psystem.VariableDeferredPConstraint;

/**
 * @author Gabor Bergmann
 *
 */
public abstract class BaseTypeSafeConstraint extends
        VariableDeferredPConstraint {

    protected Set<PVariable> inputVariables;
    protected PVariable outputVariable;

    public PVariable getOutputVariable() {
        return outputVariable;
    }

    /**
     * @param pBody
     * @param inputVariables
     * @param outputVariable null iff no output (check-only)
     */
    public BaseTypeSafeConstraint(PBody pBody,
            Set<PVariable> inputVariables, final PVariable outputVariable) {
        super(pBody,
                (outputVariable == null) ?
                        inputVariables :
                            Stream.concat(inputVariables.stream(), Stream.of(outputVariable)).collect(Collectors.toSet())
                    );
        this.inputVariables = inputVariables;
        this.outputVariable = outputVariable;
    }

    @Override
    public Set<PVariable> getDeducedVariables() {
        if (outputVariable == null)
            return Collections.emptySet();
        else
            return Collections.singleton(outputVariable);
    }

    @Override
    public Set<PVariable> getDeferringVariables() {
        return inputVariables;
    }

    @Override
    public boolean isReadyAt(SubPlan plan, IQueryMetaContext context) {
        if (super.isReadyAt(plan, context)) {
            return checkTypeSafety(plan, context) == null;
        }
        return false;
    }

    /**
     * Checks whether all type restrictions are already enforced on affected variables.
     *
     * @param plan
     * @return a variable whose type safety is not enforced yet, or null if the plan is typesafe
     */
    public PVariable checkTypeSafety(SubPlan plan, IQueryMetaContext context) {
        Set<TypeJudgement> impliedJudgements = plan.getAllImpliedTypeJudgements(context);

        for (PVariable pVariable : inputVariables) {
            Set<TypeJudgement> allTypeRestrictionsForVariable = pBody.getAllUnaryTypeRestrictions(context).get(pVariable);
            if (allTypeRestrictionsForVariable != null && !impliedJudgements.containsAll(allTypeRestrictionsForVariable))
                return pVariable;
        }
        return null;
    }

    @Override
    protected void doReplaceVariable(PVariable obsolete, PVariable replacement) {
        if (inputVariables.remove(obsolete))
            inputVariables.add(replacement);
        if (outputVariable == obsolete)
            outputVariable = replacement;
    }
}
