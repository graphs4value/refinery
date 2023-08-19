/*******************************************************************************
 * Copyright (c) 2010-2016, Grill Balázs, IncQuery Labs Ltd.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.viatra.runtime.localsearch.plan;

import tools.refinery.viatra.runtime.localsearch.matcher.MatcherReference;
import tools.refinery.viatra.runtime.localsearch.matcher.integration.LocalSearchHints;
import tools.refinery.viatra.runtime.localsearch.planner.compiler.IOperationCompiler;
import tools.refinery.viatra.runtime.matchers.ViatraQueryRuntimeException;
import tools.refinery.viatra.runtime.matchers.backend.ResultProviderRequestor;
import tools.refinery.viatra.runtime.matchers.context.IQueryBackendContext;

/**
 * @author Grill Balázs
 * @since 1.4
 * @noreference This interface is not intended to be referenced by clients.
 */
public interface IPlanProvider {

    /**
     * @throws ViatraQueryRuntimeException
     * @since 2.1
     */
    public IPlanDescriptor getPlan(IQueryBackendContext backend, IOperationCompiler compiler, 
            ResultProviderRequestor resultProviderRequestor,
            LocalSearchHints configuration, MatcherReference key);
    
}
