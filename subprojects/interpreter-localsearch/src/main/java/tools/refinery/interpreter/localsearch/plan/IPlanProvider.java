/*******************************************************************************
 * Copyright (c) 2010-2016, Grill Balázs, IncQuery Labs Ltd.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.interpreter.localsearch.plan;

import tools.refinery.interpreter.localsearch.matcher.MatcherReference;
import tools.refinery.interpreter.localsearch.matcher.integration.LocalSearchHints;
import tools.refinery.interpreter.localsearch.planner.compiler.IOperationCompiler;
import tools.refinery.interpreter.matchers.InterpreterRuntimeException;
import tools.refinery.interpreter.matchers.backend.ResultProviderRequestor;
import tools.refinery.interpreter.matchers.context.IQueryBackendContext;

/**
 * @author Grill Balázs
 * @since 1.4
 * @noreference This interface is not intended to be referenced by clients.
 */
public interface IPlanProvider {

    /**
     * @throws InterpreterRuntimeException
     * @since 2.1
     */
    public IPlanDescriptor getPlan(IQueryBackendContext backend, IOperationCompiler compiler,
                                   ResultProviderRequestor resultProviderRequestor,
                                   LocalSearchHints configuration, MatcherReference key);

}
