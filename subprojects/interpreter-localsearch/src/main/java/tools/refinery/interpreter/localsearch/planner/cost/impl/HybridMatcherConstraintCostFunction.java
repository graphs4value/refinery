/*******************************************************************************
 * Copyright (c) 2010-2016, Grill Balázs, IncQuery Labs Ltd
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.interpreter.localsearch.planner.cost.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import tools.refinery.interpreter.localsearch.planner.cost.IConstraintEvaluationContext;
import tools.refinery.interpreter.matchers.backend.IQueryResultProvider;
import tools.refinery.interpreter.matchers.psystem.PConstraint;
import tools.refinery.interpreter.matchers.psystem.PVariable;
import tools.refinery.interpreter.matchers.psystem.basicenumerables.ConstantValue;
import tools.refinery.interpreter.matchers.psystem.basicenumerables.PositivePatternCall;
import tools.refinery.interpreter.matchers.tuple.TupleMask;
import tools.refinery.interpreter.matchers.tuple.Tuple;
import tools.refinery.interpreter.matchers.tuple.Tuples;

/**
 * This cost function is intended to be used on hybrid configuration, with the strict restriction than any
 * non-flattened positive pattern call is executed with Rete engine. This implementation provides the exact number
 * of matches by invoking the result provider for the called pattern.
 *
 * @deprecated {@link StatisticsBasedConstraintCostFunction} should use {@link IQueryResultProvider#estimateCardinality(TupleMask, org.eclipse.viatra.query.runtime.matchers.util.Accuracy)}
 */
@Deprecated
public class HybridMatcherConstraintCostFunction extends IndexerBasedConstraintCostFunction {

    @Override
    protected double _calculateCost(PositivePatternCall patternCall, IConstraintEvaluationContext context) {
        // Determine local constant constraints which is used to filter results
        Tuple variables = patternCall.getVariablesTuple();
        Set<Object> variablesSet = variables.getDistinctElements();
        final Map<PVariable, Object> constantMap = new HashMap<>();
        for (PConstraint _constraint : patternCall.getPSystem().getConstraints()) {
            if (_constraint instanceof ConstantValue){
                ConstantValue constraint = (ConstantValue) _constraint;
                PVariable variable = (PVariable) constraint.getVariablesTuple().get(0);
                if (variablesSet.contains(variable) && context.getBoundVariables().contains(variable)) {
                    constantMap.put(variable, constraint.getSupplierKey());
                }
            }
        }

        // Determine filter
        Object[] filter = new Object[variables.getSize()];
        for(int i=0; i < variables.getSize(); i++){
            filter[i] = constantMap.get(variables.get(i));
        }

        // aggregate keys are the bound and not filtered variables
        // These will be fixed in runtime, but unknown at planning time
        // This is represented by indices to ease working with result tuples
        final Map<Object, Integer> variableIndices = variables.invertIndex();
        List<Integer> aggregateKeys = context.getBoundVariables().stream()
                .filter(input -> !constantMap.containsKey(input))
                .map(variableIndices::get)
                .collect(Collectors.toList());

        IQueryResultProvider resultProvider = context.resultProviderRequestor().requestResultProvider(patternCall, null);
        Map<Tuple, Integer> aggregatedCounts = new HashMap<>();

        // Iterate over all matches and count together matches that has equal values on
        // aggregateKeys positions. The cost of the pattern call is considered to be the
        // Maximum of these counted values

        int result = 0;
        // NOTE: a stream is not an iterable (cannot be iterated more than once), so to use it in a for-loop
        // it has to be wrapped; in the following line a lambda is used to implement Iterable#iterator()
        for (Tuple match : (Iterable<Tuple>) () -> resultProvider.getAllMatches(filter).iterator()) {
            Tuple extracted = Tuples.flatTupleOf(aggregateKeys.stream().map(match::get).toArray());
            int count = (aggregatedCounts.containsKey(extracted))
                ? aggregatedCounts.get(extracted) + 1
                : 1;
            aggregatedCounts.put(extracted, count);
            if (result < count) {
                result = count;
            }
        }

        return result;
    }

}
