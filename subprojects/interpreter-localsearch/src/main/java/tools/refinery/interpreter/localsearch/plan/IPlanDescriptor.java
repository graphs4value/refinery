/*******************************************************************************
 * Copyright (c) 2010-2016, Grill Balázs, IncQuery Labs Ltd.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.interpreter.localsearch.plan;

import java.util.Collection;
import java.util.Set;

import tools.refinery.interpreter.matchers.context.IInputKey;
import tools.refinery.interpreter.matchers.psystem.queries.PParameter;
import tools.refinery.interpreter.matchers.psystem.queries.PQuery;

/**
 * Denotes an executable plan
 *
 * @author Grill Balázs
 * @since 1.4
 *
 */
public interface IPlanDescriptor {

    /**
     * The query which this plan implements
     */
    public PQuery getQuery();

    /**
     * The executable search plans for each body in the query
     * @since 2.0
     */
    public Collection<SearchPlanForBody> getPlan();

    /**
     * The set of parameters this plan assumes to be bound
     */
    public Set<PParameter> getAdornment();

    /**
     * The collection of {@link IInputKey}s which needs to be iterated during the execution of this plan. For optimal
     * performance, instances of these keys might be indexed.
     */
    public Set<IInputKey> getIteratedKeys();

}
