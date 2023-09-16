/*******************************************************************************
 * Copyright (c) 2010-2016, Grill Balázs, IncQueryLabs Ltd.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.interpreter.localsearch.matcher.integration;

import java.util.Collections;
import java.util.Set;

import tools.refinery.interpreter.matchers.backend.QueryEvaluationHint;
import tools.refinery.interpreter.matchers.backend.QueryEvaluationHint.BackendRequirement;
import tools.refinery.interpreter.matchers.psystem.queries.PParameter;
import tools.refinery.interpreter.matchers.psystem.queries.PQuery;

/**
 * An adornment provider is used to define the adornments the pattern matcher should prepare for.
 *
 * <p>A default implementation is available in {@link AllValidAdornments} that describes all
 * adornments fulfilling the parameter direction declarations;
 * another default option (with better performance but restricted applicability) is {@link LazyPlanningAdornments}.
 *
 * <br><br>
 *
 * Users may implement this interface to limit the number of prepared plans based on some runtime information:
 *
 * <pre>
 * class SomeAdornments{
 *
 *     public Iterable&lt;Set&lt;{@link PParameter}>> getAdornments({@link PQuery} query){
 *         if (SomeGeneratedQuerySpecification.instance().getInternalQueryRepresentation().equals(query)){
 *             return Collections.singleton(Sets.filter(Sets.newHashSet(query.getParameters()), new Predicate<PParameter>() {
 *
 *                  &#64;Override
 *                  public boolean apply(PParameter input) {
 *                      // Decide whether this particular parameter will be bound
 *                      return false;
 *                  }
 *              }));
 *         }
 *         // Returning an empty iterable is safe for unknown queries
 *         return Collections.emptySet();
 *     }
 *
 * }
 * </pre>
 *
 * @author Grill Balázs
 * @since 1.5
 *
 */
public interface IAdornmentProvider {

    /**
     * The bound parameter sets
     */
    public Iterable<Set<PParameter>> getAdornments(PQuery query);

    /**
     * @return a simple hint that only overrides the adornment provider
     * @since 2.1
     */
    public static QueryEvaluationHint toHint(IAdornmentProvider adornmentProvider) {
        return new QueryEvaluationHint(
                Collections.singletonMap(LocalSearchHintOptions.ADORNMENT_PROVIDER, adornmentProvider),
                BackendRequirement.UNSPECIFIED);
    }

}
