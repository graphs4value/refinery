/*******************************************************************************
 * Copyright (c) 2010-2016, Grill Balázs, IncQueryLabs Ltd.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.interpreter.matchers.context;

import tools.refinery.interpreter.matchers.psystem.queries.PQuery;
import tools.refinery.interpreter.matchers.backend.IQueryResultProvider;
import tools.refinery.interpreter.matchers.backend.QueryEvaluationHint;

/**
 * This interface exposes API to request {@link IQueryResultProvider} for {@link PQuery} instances.
 *
 * @author Grill Balázs
 * @since 1.5
 * @noimplement This interface is not intended to be implemented by clients.
 */
public interface IQueryResultProviderAccess {

    /**
     * Get a result provider for the given {@link PQuery}, which conforms the capabilities requested by the
     * given {@link QueryEvaluationHint} object.
     * @throws ViatraQueryRuntimeException
     */
    public IQueryResultProvider getResultProvider(PQuery query, QueryEvaluationHint overrideHints);

}
