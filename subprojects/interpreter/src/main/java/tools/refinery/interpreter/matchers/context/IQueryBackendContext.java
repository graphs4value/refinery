/*******************************************************************************
 * Copyright (c) 2010-2016, Grill Balázs, IncQueryLabs Ltd.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.interpreter.matchers.context;

import org.apache.log4j.Logger;
import tools.refinery.interpreter.matchers.psystem.analysis.QueryAnalyzer;
import tools.refinery.interpreter.matchers.psystem.queries.PQuery;
import tools.refinery.interpreter.matchers.backend.IMatcherCapability;
import tools.refinery.interpreter.matchers.backend.IQueryBackendHintProvider;
import tools.refinery.interpreter.matchers.backend.QueryEvaluationHint;

/**
 * This interface is a collector which holds every API that is provided by the engine to control
 * the operation of the backends.
 *
 * @since 1.5
 * @noimplement This interface is not intended to be implemented by clients.
 */
public interface IQueryBackendContext {

    Logger getLogger();

    IQueryRuntimeContext getRuntimeContext();

    IQueryCacheContext getQueryCacheContext();

    IQueryBackendHintProvider getHintProvider();

    IQueryResultProviderAccess getResultProviderAccess();

    QueryAnalyzer getQueryAnalyzer();

    /**
     * @since 2.0
     */
    IMatcherCapability getRequiredMatcherCapability(PQuery query, QueryEvaluationHint overrideHints);

    /**
     * @since 1.6
     */
    boolean areUpdatesDelayed();

}
