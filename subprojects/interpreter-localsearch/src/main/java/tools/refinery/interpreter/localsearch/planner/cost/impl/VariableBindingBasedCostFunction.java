/*******************************************************************************
 * Copyright (c) 2010-2014, Marton Bur, Balazs Grill, Akos Horvath, Zoltan Ujhelyi, Istvan Rath and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.interpreter.localsearch.planner.cost.impl;

import java.util.Set;

import tools.refinery.interpreter.localsearch.planner.cost.IConstraintEvaluationContext;
import tools.refinery.interpreter.localsearch.planner.cost.ICostFunction;
import tools.refinery.interpreter.matchers.psystem.PConstraint;
import tools.refinery.interpreter.matchers.psystem.PVariable;
import tools.refinery.interpreter.matchers.psystem.basicdeferred.AggregatorConstraint;
import tools.refinery.interpreter.matchers.psystem.basicdeferred.ExportedParameter;
import tools.refinery.interpreter.matchers.psystem.basicdeferred.NegativePatternCall;
import tools.refinery.interpreter.matchers.psystem.basicenumerables.BinaryTransitiveClosure;
import tools.refinery.interpreter.matchers.psystem.basicenumerables.ConstantValue;

/**
 * This class can be used to calculate cost of application of a constraint with a given adornment.
 *
 * For now the logic is based on the following principles:
 *
 * <li>The transitive closures, NACs and count finds are the most expensive operations
 *
 * <li>The number of free variables increase the cost
 *
 * <li>If all the variables of a constraint are free, then its cost equals to twice the number of its parameter
 * variables. This solves the problem of unnecessary iteration over instances at the beginning of a plan (thus causing
 * very long run times when executing the plan) by applying constraints based on structural features as soon as
 * possible.
 *
 * <br>
 *
 * @author Marton Bur
 * @since 1.4
 *
 */
public class VariableBindingBasedCostFunction implements ICostFunction {

    // Static cost definitions
    private static int MAX = 1000;
    private static int exportedParameterCost = MAX - 20;
    private static int binaryTransitiveClosureCost = MAX - 50;
    private static int nacCost = MAX - 100;
    private static int aggregatorCost = MAX - 200;
    private static int constantCost = 0;

    @Override
    public double apply(IConstraintEvaluationContext input) {
        PConstraint constraint = input.getConstraint();
        Set<PVariable> affectedVariables = constraint.getAffectedVariables();

        int cost = 0;

        // For constants the cost is determined to be 0.0
        // The following constraints should be checks:
        // * Binary transitive closure
        // * NAC
        // * count
        // * exported parameter - only a metadata
        if (constraint instanceof ConstantValue) {
            cost = constantCost;
        } else if (constraint instanceof BinaryTransitiveClosure) {
            cost = binaryTransitiveClosureCost;
        } else if (constraint instanceof NegativePatternCall) {
            cost = nacCost;
        } else if (constraint instanceof AggregatorConstraint) {
            cost = aggregatorCost;
        } else if (constraint instanceof ExportedParameter) {
            cost = exportedParameterCost;
        } else {
            // In case of other constraints count the number of unbound variables
            for (PVariable pVariable : affectedVariables) {
                if (input.getFreeVariables().contains(pVariable)) {
                    // For each free variable ('without-value-variable') increase cost
                    cost += 1;
                }
            }
            if (cost == affectedVariables.size()) {
                // If all the variables are free, double the cost.
                // This ensures that iteration costs more
                cost *= 2;
            }

        }

        return Float.valueOf(cost);
    }

}
