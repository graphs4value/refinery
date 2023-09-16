/*******************************************************************************
 * Copyright (c) 2010-2014, Zoltan Ujhelyi, Istvan Rath and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.interpreter.matchers.psystem.queries;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

import tools.refinery.interpreter.matchers.psystem.PBody;

/**
 *
 * A disjunction is a set of bodies representing separate conditions. A {@link PQuery} has a single, canonical
 * PDisjunction, that can be replaced using rewriter
 *
 * @author Zoltan Ujhelyi
 *
 */
public class PDisjunction {

    private Set<PBody> bodies;
    private PQuery query;

    public PDisjunction(Set<PBody> bodies) {
        this(bodies.iterator().next().getPattern(), bodies);
    }

    public PDisjunction(PQuery query, Set<PBody> bodies) {
        super();
        this.query = query;
        this.bodies = Collections.unmodifiableSet(new LinkedHashSet<>(bodies));
        this.bodies.forEach(body -> body.setContainerDisjunction(this));
    }

    /**
     * Returns an immutable set of bodies that consists of this disjunction
     *
     * @return the bodies
     */
    public Set<PBody> getBodies() {
        return bodies;
    }

    /**
     * Returns the corresponding query specification. May be null if not set.
     */
    public PQuery getQuery() {
        return query;
    }

    /**
     * Returns all queries directly referred in the constraints. They are all required to evaluate this query
     *
     * @return a non-null, but possibly empty list of query definitions
     */
    public Set<PQuery> getDirectReferredQueries() {
        return this.getBodies().stream().
                flatMap(PQueries.directlyReferencedQueriesFunction()). // flatten stream of streams
                collect(Collectors.toCollection(LinkedHashSet::new));
    }

    /**
     * Returns all queries required to evaluate this query (transitively).
     *
     * @return a non-null, but possibly empty list of query definitions
     */
    public Set<PQuery> getAllReferredQueries() {
        Set<PQuery> processedQueries = new LinkedHashSet<>();
        processedQueries.add(this.getQuery());
        Set<PQuery> foundQueries = getDirectReferredQueries();
        Set<PQuery> newQueries = new LinkedHashSet<>(foundQueries);

        while(!processedQueries.containsAll(newQueries)) {
            PQuery query = newQueries.iterator().next();
            processedQueries.add(query);
            newQueries.remove(query);
            Set<PQuery> referred = query.getDirectReferredQueries();
            referred.removeAll(processedQueries);
            foundQueries.addAll(referred);
            newQueries.addAll(referred);
        }
        return foundQueries;
    }

    /**
     * Decides whether a disjunction is mutable. A disjunction is mutable if all its contained bodies are mutable.
     *
     */
    public boolean isMutable() {
        for (PBody body : bodies) {
            if (!body.isMutable()) {
                return false;
            }
        }
        return true;
    }
}
