/*******************************************************************************
 * Copyright (c) 2010-2014, Marton Bur, Akos Horvath, Zoltan Ujhelyi, Istvan Rath and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.interpreter.matchers.psystem.rewriters;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import tools.refinery.interpreter.matchers.psystem.basicdeferred.Equality;
import tools.refinery.interpreter.matchers.psystem.basicdeferred.ExportedParameter;
import tools.refinery.interpreter.matchers.psystem.basicdeferred.ExpressionEvaluation;
import tools.refinery.interpreter.matchers.psystem.basicenumerables.PositivePatternCall;
import tools.refinery.interpreter.matchers.psystem.PBody;
import tools.refinery.interpreter.matchers.psystem.PConstraint;
import tools.refinery.interpreter.matchers.psystem.PVariable;
import tools.refinery.interpreter.matchers.psystem.queries.PQuery;
import tools.refinery.interpreter.matchers.util.Preconditions;

/**
 * This rewriter class can add new equality constraints to the copied body
 *
 * @author Marton Bur
 *
 */
class FlattenerCopier extends PBodyCopier {

    private final Map<PositivePatternCall, CallInformation> calls;

    private static class CallInformation {
        final PBody body;
        final Map<PVariable, PVariable> variableMapping;

        private CallInformation(PBody body) {
            this.body = body;
            this.variableMapping = new HashMap<>();
        }
    }

    public FlattenerCopier(PQuery query, Map<PositivePatternCall, PBody> callsToFlatten) {
        super(query);
        this.calls = callsToFlatten.entrySet().stream().collect(Collectors.toMap(Entry::getKey, entry -> new CallInformation(entry.getValue())));
    }

    protected void copyVariable(PositivePatternCall contextPatternCall, PVariable variable, String newName) {
        PVariable newPVariable = body.getOrCreateVariableByName(newName);
        calls.get(contextPatternCall).variableMapping.put(variable, newPVariable);
        variableMapping.put(variable, newPVariable);
    }

    /**
     * Merge all variables and constraints from the body called through the given pattern call to a target body. If
     * multiple bodies are merged into a single one, use the renamer and filter options to avoid collisions.
     *
     * @param sourceBody
     * @param namingTool
     * @param filter
     */
    public void mergeBody(PositivePatternCall contextPatternCall, IVariableRenamer namingTool,
            IConstraintFilter filter) {

        PBody sourceBody = calls.get(contextPatternCall).body;

        // Copy variables
        Set<PVariable> allVariables = sourceBody.getAllVariables();
        for (PVariable pVariable : allVariables) {
            if (pVariable.isUnique()) {
                copyVariable(contextPatternCall, pVariable,
                        namingTool.createVariableName(pVariable, sourceBody.getPattern()));
            }
        }

        // Copy constraints which are not filtered
        Set<PConstraint> constraints = sourceBody.getConstraints();
        for (PConstraint pConstraint : constraints) {
            if (!(pConstraint instanceof ExportedParameter) && !filter.filter(pConstraint)) {
                copyConstraint(pConstraint);
            }
        }
    }

    @Override
    protected void copyPositivePatternCallConstraint(PositivePatternCall positivePatternCall) {

        if (!calls.containsKey(positivePatternCall)) {
            // If the call was not flattened, copy the constraint
            super.copyPositivePatternCallConstraint(positivePatternCall);
        } else {
            PBody calledBody = Objects.requireNonNull(calls.get(positivePatternCall).body);
            Preconditions.checkArgument(positivePatternCall.getReferredQuery().equals(calledBody.getPattern()));

            List<PVariable> symbolicParameters = calledBody.getSymbolicParameterVariables();
            Object[] elements = positivePatternCall.getVariablesTuple().getElements();
            for (int i = 0; i < elements.length; i++) {
                // Create equality constraints between the caller PositivePatternCall and the corresponding body
                // parameter variables
                createEqualityConstraint((PVariable) elements[i], symbolicParameters.get(i), positivePatternCall);
            }

        }
    }

    private void createEqualityConstraint(PVariable pVariable1, PVariable pVariable2,
            PositivePatternCall contextPatternCall) {
        PVariable who = variableMapping.get(pVariable1);
        PVariable withWhom = calls.get(contextPatternCall).variableMapping.get(pVariable2);
        addTrace(contextPatternCall, new Equality(body, who, withWhom));
    }

    @Override
    protected void copyExpressionEvaluationConstraint(final ExpressionEvaluation expressionEvaluation) {
        Map<PVariable, PVariable> variableMapping = this.variableMapping.entrySet().stream()
                .filter(input -> expressionEvaluation.getPSystem().getAllVariables().contains(input.getKey()))
                .collect(Collectors.toMap(Entry::getKey, Entry::getValue));

        PVariable mappedOutputVariable = variableMapping.get(expressionEvaluation.getOutputVariable());
        addTrace(expressionEvaluation, new ExpressionEvaluation(body, new VariableMappingExpressionEvaluatorWrapper(expressionEvaluation.getEvaluator(), variableMapping), mappedOutputVariable, expressionEvaluation.isUnwinding()));
    }

}
