/*******************************************************************************
 * Copyright (c) 2010-2017, Grill Balázs, IncQueryLabs
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.interpreter.matchers.psystem.rewriters;

import java.util.Objects;

import tools.refinery.interpreter.matchers.psystem.PConstraint;
import tools.refinery.interpreter.matchers.psystem.PTraceable;

/**
 * @since 1.6
 *
 */
public class AbstractRewriterTraceSource {

    private IRewriterTraceCollector traceCollector = NopTraceCollector.INSTANCE;

    public void setTraceCollector(IRewriterTraceCollector traceCollector) {
        this.traceCollector = Objects.requireNonNull(traceCollector);
    }

    public IPTraceableTraceProvider getTraces() {
        return traceCollector;
    }

    protected IRewriterTraceCollector getTraceCollector() {
        return traceCollector;
    }

    /**
     * Mark the given derivative to be originated from the given original constraint.
     * @since 1.6
     */
    protected void addTrace(PTraceable original, PTraceable derivative){
        traceCollector.addTrace(original, derivative);
    }

    /**
     * Indicate that the given derivative is removed from the resulting query, thus its trace
     * information should be removed also.
     * @since 1.6
     */
    protected void derivativeRemoved(PConstraint derivative, IDerivativeModificationReason reason){
        traceCollector.derivativeRemoved(derivative, reason);
    }

}
