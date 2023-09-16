/*******************************************************************************
 * Copyright (c) 2010-2013, Zoltan Ujhelyi, Istvan Rath and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.interpreter.matchers.psystem.basicdeferred;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import tools.refinery.interpreter.matchers.context.IQueryMetaContext;
import tools.refinery.interpreter.matchers.psystem.IExpressionEvaluator;
import tools.refinery.interpreter.matchers.psystem.PBody;
import tools.refinery.interpreter.matchers.psystem.PVariable;
import tools.refinery.interpreter.matchers.tuple.Tuples;

/**
 * @author Zoltan Ujhelyi
 *
 */
public class ExpressionEvaluation extends BaseTypeSafeConstraint {

    private IExpressionEvaluator evaluator;
    private boolean isUnwinding;

    public ExpressionEvaluation(PBody pBody, IExpressionEvaluator evaluator, PVariable outputVariable) {
        this(pBody, evaluator, outputVariable, false);
    }

    /**
     * @since 2.4
     */
    public ExpressionEvaluation(PBody pBody, IExpressionEvaluator evaluator, PVariable outputVariable,
            boolean isUnwinding) {
        super(pBody, getPVariablesOfExpression(pBody, evaluator), outputVariable);
        this.evaluator = evaluator;
        this.isUnwinding = isUnwinding;
    }

    /**
     * @since 2.4
     */
    public boolean isUnwinding() {
        return isUnwinding;
    }

    public IExpressionEvaluator getEvaluator() {
        return evaluator;
    }

    @Override
    protected String toStringRest() {
        return Tuples.flatTupleOf(new ArrayList<PVariable>(inputVariables).toArray()).toString() + "|="
                + evaluator.getShortDescription();
    }

    @Override
    public Map<Set<PVariable>, Set<PVariable>> getFunctionalDependencies(IQueryMetaContext context) {
        if (outputVariable == null)
            return Collections.emptyMap();
        else
            return Collections.singletonMap(inputVariables, Collections.singleton(outputVariable));
    }

    private static Set<PVariable> getPVariablesOfExpression(PBody pBody, IExpressionEvaluator evaluator) {
        // use a linked set, so that the variables will come in the order of the parameters
        Set<PVariable> result = new LinkedHashSet<PVariable>();
        for (String name : evaluator.getInputParameterNames()) {
            PVariable variable = pBody.getOrCreateVariableByName(name);
            result.add(variable);
        }
        return result;
    }
}
