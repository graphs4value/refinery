/*******************************************************************************
 * Copyright (c) 2010-2014, Zoltan Ujhelyi, Istvan Rath and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.interpreter.matchers.psystem.queries;

import tools.refinery.interpreter.matchers.context.IInputKey;
import tools.refinery.interpreter.matchers.psystem.IMultiQueryReference;
import tools.refinery.interpreter.matchers.psystem.ITypeConstraint;
import tools.refinery.interpreter.matchers.psystem.PBody;
import tools.refinery.interpreter.matchers.psystem.PTraceable;
import tools.refinery.interpreter.matchers.psystem.basicenumerables.TypeConstraint;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * Utility class for using PQueries in functional/streaming collection operations effectively
 *
 * @author Zoltan Ujhelyi
 *
 */
public final class PQueries {

    /**
     * Hidden constructor for utility class
     */
    private PQueries() {
    }

    /**
     * Predicate checking for the status of selected queries
     *
     */
    public static Predicate<PQuery> queryStatusPredicate(final PQuery.PQueryStatus status) {
        return query -> query.getStatus().equals(status);
    }

    /**
     * Enumerates referred queries (without duplicates) for the given body
     */
    public static Function<PBody, Stream<PQuery>> directlyReferencedQueriesFunction() {
        return body -> (body.getConstraintsOfType(IMultiQueryReference.class).stream()
                .flatMap(e -> e.getReferredQueries().stream()).distinct());
    }

    /**
     * Enumerates directly referred extensional relations (without duplicates) in the canonical form of the given query
     *
     * @param enumerablesOnly
     *                            only enumerable type constraints are considered
     * @since 2.0
     */
    public static Stream<IInputKey> directlyRequiredTypesOfQuery(PQuery query, boolean enumerablesOnly) {
        return directlyRequiredTypesOfDisjunction(query.getDisjunctBodies(), enumerablesOnly);
    }

    /**
     * Enumerates directly referred extensional relations (without duplicates) for the given formulation of a query.
     *
     * @param enumerablesOnly
     *                            only enumerable type constraints are considered
     * @since 2.0
     */
    public static Stream<IInputKey> directlyRequiredTypesOfDisjunction(PDisjunction disjunctBodies,
            boolean enumerablesOnly) {
        Class<? extends ITypeConstraint> filterClass = enumerablesOnly ? TypeConstraint.class : ITypeConstraint.class;
        return disjunctBodies.getBodies().stream().flatMap(body -> body.getConstraintsOfType(filterClass).stream())
                .map(constraint -> constraint.getEquivalentJudgement().getInputKey()).distinct();
    }

    /**
     * @since 1.4
     */
    public static Predicate<PParameter> parameterDirectionPredicate(final PParameterDirection direction) {
        return input -> input.getDirection() == direction;
    }

    /**
     * Returns all {@link PTraceable}s contained in the given {@link PQuery}: itself, its bodies and their constraints.
     *
     * @since 1.6
     */
    public static Set<PTraceable> getTraceables(PQuery query) {
        final Set<PTraceable> traceables = new HashSet<>();
        traceables.add(query);
        query.getDisjunctBodies().getBodies().forEach(body -> {
            traceables.add(body);
            body.getConstraints().forEach(traceables::add);
        });
        return traceables;
    }

    /**
     * Calculates the simple name related from a given qualified name by finding the part after the last '.' character.
     *
     * @since 2.0
     */
    public static String calculateSimpleName(String qualifiedName) {
        return qualifiedName.substring(qualifiedName.lastIndexOf('.') + 1);
    }
}
