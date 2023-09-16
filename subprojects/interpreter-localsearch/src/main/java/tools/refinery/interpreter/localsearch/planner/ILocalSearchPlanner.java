/*******************************************************************************
 * Copyright (c) 2010-2017, Zoltan Ujhelyi, IncQuery Labs Ltd.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.interpreter.localsearch.planner;

import java.util.Collection;
import java.util.Set;

import tools.refinery.interpreter.localsearch.plan.SearchPlanForBody;
import tools.refinery.interpreter.matchers.InterpreterRuntimeException;
import tools.refinery.interpreter.matchers.psystem.queries.PParameter;
import tools.refinery.interpreter.matchers.psystem.queries.PQuery;

/**
 * @author Zoltan Ujhelyi
 * @since 1.7
 */
public interface ILocalSearchPlanner {

    /**
     * Creates executable plans for the provided query. It is required to call one of the
     * <code>initializePlanner()</code> methods before calling this method.
     *
     * @param querySpec
     * @param boundParameters
     *            a set of bound parameters
     * @return a mapping between ISearchOperation list and a mapping, that holds a PVariable-Integer mapping for the
     *         list of ISearchOperations
     * @throws InterpreterRuntimeException
     */
    Collection<SearchPlanForBody> plan(PQuery querySpec, Set<PParameter> boundParameters);

}
