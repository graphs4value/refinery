/*******************************************************************************
 * Copyright (c) 2010-2015, Bergmann Gabor, Istvan Rath and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.interpreter.matchers.psystem;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;

import tools.refinery.interpreter.matchers.context.IInputKey;
import tools.refinery.interpreter.matchers.context.IQueryMetaContext;
import tools.refinery.interpreter.matchers.tuple.Tuple;

import java.util.Set;

/**
 * Common superinterface of enumerable and deferred type constraints.
 * @author Bergmann Gabor
 *
 */
public interface ITypeConstraint extends ITypeInfoProviderConstraint {

    public abstract TypeJudgement getEquivalentJudgement();

    /**
     * Static internal utility class for implementations of {@link ITypeConstraint}s.
     * @author Bergmann Gabor
     */
    public static class TypeConstraintUtil {

        private TypeConstraintUtil() {
            // Hiding constructor for utility class
        }

        public static Map<Set<PVariable>, Set<PVariable>> getFunctionalDependencies(IQueryMetaContext context, IInputKey inputKey, Tuple variablesTuple) {
            final Map<Set<PVariable>, Set<PVariable>> result = new HashMap<Set<PVariable>, Set<PVariable>>();

            Set<Entry<Set<Integer>, Set<Integer>>> dependencies = context.getFunctionalDependencies(inputKey).entrySet();
            for (Entry<Set<Integer>, Set<Integer>> dependency : dependencies) {
                result.put(
                        transcribeVariables(dependency.getKey(), variablesTuple),
                        transcribeVariables(dependency.getValue(), variablesTuple)
                );
            }

            return result;
        }

        private static Set<PVariable> transcribeVariables(Set<Integer> indices, Tuple variablesTuple) {
            Set<PVariable> result = new HashSet<PVariable>();
            for (Integer index : indices) {
                result.add((PVariable) variablesTuple.get(index));
            }
            return result;
        }

    }

}
