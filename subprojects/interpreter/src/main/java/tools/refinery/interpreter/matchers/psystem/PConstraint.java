/*******************************************************************************
 * Copyright (c) 2004-2010 Gabor Bergmann and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package tools.refinery.interpreter.matchers.psystem;

import java.util.Comparator;
import java.util.Map;
import java.util.Set;

import tools.refinery.interpreter.matchers.psystem.analysis.QueryAnalyzer;
import tools.refinery.interpreter.matchers.context.IQueryMetaContext;

/**
 * @author Gabor Bergmann
 *
 */
public interface PConstraint extends PTraceable {

    /**
     * @since 2.1
     * @return the query body this constraint belongs to
     */
    public PBody getBody();

    /**
     * All variables affected by this constraint.
     */
    public Set<PVariable> getAffectedVariables();

    /**
     * The set of variables whose potential values can be enumerated (once all non-deduced variables have known values).
     */
    public Set<PVariable> getDeducedVariables();

    /**
     * A (preferably minimal) cover of known functional dependencies between variables.
     * @noreference Use {@link QueryAnalyzer} instead to properly handle dependencies of pattern calls.
     * @return non-trivial functional dependencies in the form of {variables} --> {variables}, where dependencies with the same lhs are unified.
     */
    public Map<Set<PVariable>,Set<PVariable>> getFunctionalDependencies(IQueryMetaContext context);

    public void replaceVariable(PVariable obsolete, PVariable replacement);

    public void delete();

    public void checkSanity();

    /**
     * Returns an integer ID that is guaranteed to increase strictly monotonously for constraints within a pBody.
     */
    public abstract int getMonotonousID();


    /**
     * A comparator that orders constraints by their {@link #getMonotonousID() monotonous identifiers}. Should only used
     * for tiebreaking in other comparators.
     *
     * @since 2.0
     */
    public static final Comparator<PConstraint> COMPARE_BY_MONOTONOUS_ID = (arg0, arg1) -> arg0.getMonotonousID() - arg1.getMonotonousID();



}
