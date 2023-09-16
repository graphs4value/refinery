/*******************************************************************************
 * Copyright (c) 2004-2010 Gabor Bergmann and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package tools.refinery.interpreter.matchers.planning.helpers;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Collectors;

import tools.refinery.interpreter.matchers.context.IInputKey;
import tools.refinery.interpreter.matchers.context.IQueryMetaContext;
import tools.refinery.interpreter.matchers.psystem.ITypeInfoProviderConstraint;
import tools.refinery.interpreter.matchers.psystem.PConstraint;
import tools.refinery.interpreter.matchers.psystem.PVariable;
import tools.refinery.interpreter.matchers.psystem.TypeJudgement;

/**
 * @author Gabor Bergmann
 * @author Tamas Szabo
 */
public class TypeHelper {

    private TypeHelper() {
        // Hiding constructor for utility class
    }

    /**
     * Collects the type constraints for the specified collection of variables. The type constraints consist of the
     * constraints directly enforced on the variable itself, plus all of those that the given variable is unified with
     * through equalities.
     *
     * @param variables
     *            the variables in question
     * @param constraints
     *            the constraints in the pattern body
     * @param context
     *            the query meta context
     * @return the mapping from variable to set of type constraints
     * @since 1.6
     */
    public static Map<PVariable, Set<IInputKey>> inferUnaryTypesFor(Iterable<PVariable> variables,
            Set<PConstraint> constraints, IQueryMetaContext context) {
        Map<PVariable, Set<TypeJudgement>> typeMap = TypeHelper.inferUnaryTypes(constraints, context);
        return inferUnaryTypesFor(variables, typeMap);
    }

    /**
     * Collects the type constraints for the specified collection of variables. The type constraints consist of the
     * constraints directly enforced on the variable itself, plus all of those that the given variable is unified with
     * through equalities.
     *
     * The method accepts a type map which is the result of the basic type inference from the
     * {@link TypeHelper.inferUnaryTypes} method. The purpose of this method is that the type map can be reused across
     * several calls to this method.
     *
     * @param variables
     *            the variables in question
     * @param typeMap
     *            the type map of inference results
     * @return the mapping from variable to set of type constraints
     * @since 1.6
     */
    public static Map<PVariable, Set<IInputKey>> inferUnaryTypesFor(Iterable<PVariable> variables,
            Map<PVariable, Set<TypeJudgement>> typeMap) {
        Map<PVariable, Set<IInputKey>> result = new HashMap<PVariable, Set<IInputKey>>();

        for (PVariable original : variables) {
            // it can happen that the variable was unified into an other one due to equalities
            Set<IInputKey> keys = new HashSet<IInputKey>();
            PVariable current = original;

            while (current != null) {
                Set<TypeJudgement> judgements = typeMap.get(current);
                if (judgements != null) {
                    for (TypeJudgement judgement : judgements) {
                        keys.add(judgement.getInputKey());
                    }
                }
                current = current.getDirectUnifiedInto();
            }

            result.put(original, keys);
        }

        return result;
    }

    /**
     * Infers unary type information for variables, based on the given constraints.
     *
     * Subsumptions are not taken into account.
     *
     * @param constraints
     *            the set of constraints to extract type info from
     */
    public static Map<PVariable, Set<TypeJudgement>> inferUnaryTypes(Set<PConstraint> constraints,
            IQueryMetaContext context) {
        Set<TypeJudgement> equivalentJudgements = getDirectJudgements(constraints, context);
        Set<TypeJudgement> impliedJudgements = typeClosure(equivalentJudgements, context);

        Map<PVariable, Set<TypeJudgement>> results = new HashMap<PVariable, Set<TypeJudgement>>();
        for (TypeJudgement typeJudgement : impliedJudgements) {
            final IInputKey inputKey = typeJudgement.getInputKey();
            if (inputKey.getArity() == 1) {
                PVariable variable = (PVariable) typeJudgement.getVariablesTuple().get(0);
                Set<TypeJudgement> inferredTypes = results.computeIfAbsent(variable, v -> new HashSet<>());
                inferredTypes.add(typeJudgement);
            }
        }
        return results;
    }

    /**
     * Gets direct judgements reported by constraints. No closure is applied yet.
     */
    public static Set<TypeJudgement> getDirectJudgements(Set<PConstraint> constraints, IQueryMetaContext context) {
        Set<TypeJudgement> equivalentJudgements = new HashSet<TypeJudgement>();
        for (PConstraint pConstraint : constraints) {
            if (pConstraint instanceof ITypeInfoProviderConstraint) {
                equivalentJudgements.addAll(((ITypeInfoProviderConstraint) pConstraint).getImpliedJudgements(context));
            }
        }
        return equivalentJudgements;
    }

    /**
     * Calculates the closure of a set of type judgements, with respect to supertyping.
     *
     * @return the set of all type judgements in typesToClose and all their direct and indirect supertypes
     */
    public static Set<TypeJudgement> typeClosure(Set<TypeJudgement> typesToClose, IQueryMetaContext context) {
        return typeClosure(Collections.<TypeJudgement> emptySet(), typesToClose, context);
    }

    /**
     * Calculates the closure of a set of type judgements (with respect to supertyping), where the closure has been
     * calculated before for a given base set, but not for a separate delta set.
     * <p>
     * Precondition: the set (typesToClose MINUS delta) is already closed w.r.t. supertyping.
     *
     * @return the set of all type judgements in typesToClose and all their direct and indirect supertypes
     * @since 1.6
     */
    public static Set<TypeJudgement> typeClosure(Set<TypeJudgement> preclosedBaseSet, Set<TypeJudgement> delta,
            IQueryMetaContext context) {
        Queue<TypeJudgement> queue = delta.stream().filter(input -> !preclosedBaseSet.contains(input)).collect(Collectors.toCollection(LinkedList::new));
        if (queue.isEmpty())
            return preclosedBaseSet;

        Set<TypeJudgement> closure = new HashSet<TypeJudgement>(preclosedBaseSet);

        Map<TypeJudgement, Set<TypeJudgement>> conditionalImplications = new HashMap<>();
        for (TypeJudgement typeJudgement : closure) {
            conditionalImplications.putAll(typeJudgement.getConditionalImpliedJudgements(context));
        }

        do {
            TypeJudgement deltaType = queue.poll();
            if (closure.add(deltaType)) {
                // direct implications
                queue.addAll(deltaType.getDirectlyImpliedJudgements(context));

                // conditional implications, source key processed before, this is the condition key
                final Set<TypeJudgement> implicationSet = conditionalImplications.get(deltaType);
                if (implicationSet != null) {
                     queue.addAll(implicationSet);
                }

                // conditional implications, this is the source key
                Map<TypeJudgement, Set<TypeJudgement>> deltaConditionalImplications = deltaType
                        .getConditionalImpliedJudgements(context);
                for (Entry<TypeJudgement, Set<TypeJudgement>> entry : deltaConditionalImplications.entrySet()) {
                    if (closure.contains(entry.getKey())) {
                        // condition processed before
                        queue.addAll(entry.getValue());
                    } else {
                        // condition not processed yet
                        conditionalImplications.computeIfAbsent(entry.getKey(), key -> new HashSet<>())
                                .addAll(entry.getValue());
                    }
                }
            }
        } while (!queue.isEmpty());

        return closure;
    }

    /**
     * Calculates a remainder set of types from a larger set, that are not subsumed by a given set of subsuming types.
     *
     * @param subsumableTypes
     *            a set of types from which some may be implied by the subsuming types
     * @param subsumingTypes
     *            a set of types that may imply some of the subsuming types
     * @return the collection of types in subsumableTypes that are NOT identical to or supertypes of any type in
     *         subsumingTypes.
     */
    public static Set<TypeJudgement> subsumeTypes(Set<TypeJudgement> subsumableTypes, Set<TypeJudgement> subsumingTypes,
            IQueryMetaContext context) {
        Set<TypeJudgement> closure = typeClosure(subsumingTypes, context);
        Set<TypeJudgement> subsumed = new HashSet<TypeJudgement>(subsumableTypes);
        subsumed.removeAll(closure);
        return subsumed;
    }
}
