/*******************************************************************************
 * Copyright (c) 2010-2016, Grill Balázs, IncQuery Labs Ltd.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.interpreter.localsearch.planner.cost;

import tools.refinery.interpreter.matchers.backend.ResultProviderRequestor;
import tools.refinery.interpreter.matchers.context.IQueryResultProviderAccess;
import tools.refinery.interpreter.matchers.context.IQueryRuntimeContext;
import tools.refinery.interpreter.matchers.psystem.PConstraint;
import java.util.Collection;
import tools.refinery.interpreter.matchers.psystem.PVariable;
import tools.refinery.interpreter.matchers.psystem.analysis.QueryAnalyzer;

/**
 * This interface denotes the evaluation context of a constraint, intended for cost estimation. Provides access to information
 * on which the cost function can base its calculation.
 *
 * @author Grill Balázs
 * @since 1.4
 * @noimplement
 */
public interface IConstraintEvaluationContext {

    /**
     * Get the constraint to be evaluated
     */
    public PConstraint getConstraint();

    /**
     * Unbound variables at the time of evaluating the constraint
     */
    public Collection<PVariable> getFreeVariables();

    /**
     * Bound variables at the time of evaluating the constraint
     */
    public Collection<PVariable> getBoundVariables();

    public IQueryRuntimeContext getRuntimeContext();

    /**
     * @since 1.5
     */
    public QueryAnalyzer getQueryAnalyzer();

    /**
     * @deprecated use {@link #resultProviderRequestor()}
     * @since 1.5
     */
    @Deprecated
    public IQueryResultProviderAccess resultProviderAccess();

    /**
     * @since 2.1
     */
    public ResultProviderRequestor resultProviderRequestor();

}
