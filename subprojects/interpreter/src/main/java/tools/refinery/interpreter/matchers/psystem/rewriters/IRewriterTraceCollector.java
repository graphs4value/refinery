/*******************************************************************************
 * Copyright (c) 2010-2017, Grill Balázs, IncQueryLabs
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.interpreter.matchers.psystem.rewriters;

import tools.refinery.interpreter.matchers.psystem.PTraceable;

/**
 * This is the internal API of {@link IPTraceableTraceProvider} expected to be used by
 * copier and rewriter implementations.
 *
 * @since 1.6
 * @noreference This interface is not intended to be referenced by clients.
 */
public interface IRewriterTraceCollector extends IPTraceableTraceProvider {

    /**
     * Mark the given derivative to be originated from the given original constraint.
     */
    public void addTrace(PTraceable origin, PTraceable derivative);

    /**
     * Indicate that the given derivative is removed from the resulting query, thus its trace
     * information should be removed also.
     */
    public void derivativeRemoved(PTraceable derivative, IDerivativeModificationReason reason);

}
