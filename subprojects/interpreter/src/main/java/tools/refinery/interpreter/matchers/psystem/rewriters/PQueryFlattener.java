/*******************************************************************************
 * Copyright (c) 2010-2014, Marton Bur, Akos Horvath, Zoltan Ujhelyi, Istvan Rath and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.interpreter.matchers.psystem.rewriters;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import tools.refinery.interpreter.matchers.psystem.basicenumerables.PositivePatternCall;
import tools.refinery.interpreter.matchers.psystem.PBody;
import tools.refinery.interpreter.matchers.psystem.PConstraint;
import tools.refinery.interpreter.matchers.psystem.queries.PDisjunction;
import tools.refinery.interpreter.matchers.psystem.queries.PQuery;
import tools.refinery.interpreter.matchers.psystem.queries.PQuery.PQueryStatus;
import tools.refinery.interpreter.matchers.psystem.rewriters.IConstraintFilter.AllowAllFilter;
import tools.refinery.interpreter.matchers.psystem.rewriters.IConstraintFilter.ExportedParameterFilter;
import tools.refinery.interpreter.matchers.util.Preconditions;
import tools.refinery.interpreter.matchers.util.Sets;

/**
 * This rewriter class holds the query flattening logic
 *
 * @author Marton Bur
 *
 */
public class PQueryFlattener extends PDisjunctionRewriter {

    /**
     * Utility function to produce the permutation of every possible mapping of values.
     *
     * @param values
     * @return
     */
    private static <K, V> Set<Map<K, V>> permutation(Map<K, Set<V>> values) {
        // An ordering of keys is defined here which will help restoring the appropriate values after the execution of
        // the cartesian product
        List<K> keyList = new ArrayList<>(values.keySet());

        // Produce list of value sets with the ordering defined by keyList
        List<Set<V>> valuesList = new ArrayList<Set<V>>(keyList.size());
        for (K key : keyList) {
            valuesList.add(values.get(key));
        }

        // Cartesian product will obey ordering of the list
        Set<List<V>> valueMappings = Sets.cartesianProduct(valuesList);

        // Build result
        Set<Map<K, V>> result = new LinkedHashSet<>();
        for (List<V> valueList : valueMappings) {
            Map<K, V> map = new HashMap<>();
            for (int i = 0; i < keyList.size(); i++) {
                map.put(keyList.get(i), valueList.get(i));
            }
            result.add(map);
        }

        return result;
    }

    private IFlattenCallPredicate flattenCallPredicate;

    public PQueryFlattener(IFlattenCallPredicate flattenCallPredicate) {
        this.flattenCallPredicate = flattenCallPredicate;
    }

    @Override
    public PDisjunction rewrite(PDisjunction disjunction) {
        PQuery query = disjunction.getQuery();

        // Check for recursion
        Set<PQuery> allReferredQueries = disjunction.getAllReferredQueries();
        for (PQuery referredQuery : allReferredQueries) {
            if (referredQuery.getAllReferredQueries().contains(referredQuery)) {
                throw new RewriterException("Recursive queries are not supported, can't flatten query named \"{1}\"",
                        new String[] { query.getFullyQualifiedName() }, "Unsupported recursive query", query);
            }
        }

        return this.doFlatten(disjunction);
    }

    /**
     * Return the list of dependencies (including the root) in chronological order
     *
     * @param rootDisjunction
     * @return
     */
    private List<PDisjunction> disjunctionDependencies(PDisjunction rootDisjunction) {
        // Disjunctions are first collected into a list usign a depth-first approach,
        // which can be iterated backwards while removing duplicates
        Deque<PDisjunction> stack = new ArrayDeque<>();
        LinkedList<PDisjunction> list = new LinkedList<>();
        stack.push(rootDisjunction);
        list.add(rootDisjunction);

        while (!stack.isEmpty()) {
            PDisjunction disjunction = stack.pop();
            // Collect dependencies
            for (PBody pBody : disjunction.getBodies()) {
                for (PConstraint constraint : pBody.getConstraints()) {
                    if (constraint instanceof PositivePatternCall) {
                        PositivePatternCall positivePatternCall = (PositivePatternCall) constraint;
                        if (flattenCallPredicate.shouldFlatten(positivePatternCall)) {
                            // If the above preconditions meet, the call should be flattened
                            PDisjunction calledDisjunction = positivePatternCall.getReferredQuery().getDisjunctBodies();
                            stack.push(calledDisjunction);
                            list.add(calledDisjunction);
                        }
                    }
                }
            }
        }

        // Remove duplicates (keeping the last instance) and reverse order
        Set<PDisjunction> visited = new HashSet<PDisjunction>();
        List<PDisjunction> result = new ArrayList<PDisjunction>(list.size());

        list.descendingIterator().forEachRemaining(item -> {
            if (!visited.contains(item)) {
                result.add(item);
                visited.add(item);
            }

        });

        return result;
    }

    /**
     * This function holds the actual flattening logic for a PQuery
     *
     * @param rootDisjunction
     *            to be flattened
     * @return the flattened bodies of the pQuery
     */
    private PDisjunction doFlatten(PDisjunction rootDisjunction) {

        Map<PDisjunction, Set<PBody>> flatBodyMapping = new HashMap<>();

        List<PDisjunction> dependencies = disjunctionDependencies(rootDisjunction);

        for (PDisjunction disjunction : dependencies) {
            Set<PBody> flatBodies = new LinkedHashSet<>();
            for (PBody body : disjunction.getBodies()) {
                if (isFlatteningNeeded(body)) {
                    Map<PositivePatternCall, Set<PBody>> flattenedBodies = new HashMap<>();
                    for (PConstraint pConstraint : body.getConstraints()) {

                        if (pConstraint instanceof PositivePatternCall) {
                            PositivePatternCall positivePatternCall = (PositivePatternCall) pConstraint;
                            if (flattenCallPredicate.shouldFlatten(positivePatternCall)) {
                                // If the above preconditions meet, do the flattening and return the disjoint bodies
                                PDisjunction calledDisjunction = positivePatternCall.getReferredQuery()
                                        .getDisjunctBodies();

                                Set<PBody> flattenedBodySet = flatBodyMapping.get(calledDisjunction);
                                Preconditions.checkArgument(!flattenedBodySet.isEmpty());
                                flattenedBodies.put(positivePatternCall, flattenedBodySet);
                            }
                        }
                    }
                    flatBodies.addAll(createSetOfFlatPBodies(body, flattenedBodies));
                } else {
                    flatBodies.add(prepareFlatPBody(body));
                }
            }
            flatBodyMapping.put(disjunction, flatBodies);
        }

        return new PDisjunction(rootDisjunction.getQuery(), flatBodyMapping.get(rootDisjunction));
    }

    /**
     * Creates the flattened bodies based on the caller body and the called (and already flattened) disjunctions
     *
     * @param pBody
     *            the body to flatten
     * @param flattenedDisjunctions
     *            the
     * @param flattenedCalls
     * @return
     */
    private Set<PBody> createSetOfFlatPBodies(PBody pBody, Map<PositivePatternCall, Set<PBody>> flattenedCalls) {
        PQuery pQuery = pBody.getPattern();

        Set<Map<PositivePatternCall, PBody>> conjunctedCalls = permutation(flattenedCalls);

        // The result set containing the merged conjuncted bodies
        Set<PBody> conjunctedBodies = new HashSet<>();

        for (Map<PositivePatternCall, PBody> calledBodies : conjunctedCalls) {
            FlattenerCopier copier = createBodyCopier(pQuery, calledBodies);

            int i = 0;
            IVariableRenamer.HierarchicalName hierarchicalNamingTool = new IVariableRenamer.HierarchicalName();
            for (PositivePatternCall patternCall : calledBodies.keySet()) {
                // Merge each called body
                hierarchicalNamingTool.setCallCount(i++);
                copier.mergeBody(patternCall, hierarchicalNamingTool, new ExportedParameterFilter());
            }

            // Merge the caller's constraints to the conjunct body
            copier.mergeBody(pBody);

            PBody copiedBody = copier.getCopiedBody();
            copiedBody.setStatus(PQueryStatus.OK);
            conjunctedBodies.add(copiedBody);
        }

        return conjunctedBodies;
    }

    private FlattenerCopier createBodyCopier(PQuery query, Map<PositivePatternCall, PBody> calledBodies) {
        FlattenerCopier flattenerCopier = new FlattenerCopier(query, calledBodies);
        flattenerCopier.setTraceCollector(getTraceCollector());
        return flattenerCopier;
    }

    private PBody prepareFlatPBody(PBody pBody) {
        PBodyCopier copier = createBodyCopier(pBody.getPattern(), Collections.<PositivePatternCall, PBody> emptyMap());
        copier.mergeBody(pBody, new IVariableRenamer.SameName(), new AllowAllFilter());
        // the copying of the body here is necessary for only one containing PDisjunction can be assigned to a PBody
        return copier.getCopiedBody();
    }

    private boolean isFlatteningNeeded(PBody pBody) {
        // Check if the body contains positive pattern call AND if it should be flattened
        for (PConstraint pConstraint : pBody.getConstraints()) {
            if (pConstraint instanceof PositivePatternCall) {
                return flattenCallPredicate.shouldFlatten((PositivePatternCall) pConstraint);
            }
        }
        return false;
    }

}
