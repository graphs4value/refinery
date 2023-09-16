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

import org.apache.log4j.Logger;
import tools.refinery.interpreter.localsearch.matcher.MatcherReference;
import tools.refinery.interpreter.localsearch.matcher.integration.LocalSearchHints;
import tools.refinery.interpreter.localsearch.planner.LocalSearchPlanner;
import tools.refinery.interpreter.localsearch.planner.compiler.IOperationCompiler;
import tools.refinery.interpreter.matchers.backend.ResultProviderRequestor;
import tools.refinery.interpreter.matchers.context.IQueryBackendContext;

/**
 * A plan provider implementation which caches previously calculated plans to avoid re-planning for the same adornment
 *
 * @author Grill Balázs
 * @since 1.7
 *
 */
public class SimplePlanProvider implements IPlanProvider {

    private final Logger logger;

    public SimplePlanProvider(Logger logger) {
        this.logger = logger;
    }

    @Override
    public IPlanDescriptor getPlan(IQueryBackendContext backend, IOperationCompiler compiler,
                                   final ResultProviderRequestor resultRequestor,
                                   final LocalSearchHints configuration, MatcherReference key) {

        LocalSearchPlanner planner = new LocalSearchPlanner(backend, compiler, logger, configuration, resultRequestor);

        Collection<SearchPlanForBody> plansForBodies = planner.plan(key.getQuery(), key.getAdornment());

        IPlanDescriptor plan = new PlanDescriptor(key.getQuery(), plansForBodies, key.getAdornment());
        return plan;
    }

}
