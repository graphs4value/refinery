/*******************************************************************************
 * Copyright (c) 2010-2017, Grill Balázs, IncQueryLabs
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.interpreter.matchers.psystem.rewriters;

import java.util.stream.Stream;

import tools.refinery.interpreter.matchers.psystem.PTraceable;

/**
 * This implementation does not store any traces and scales to NOP for every traceability feature.
 * @since 1.6
 *
 */
public class NopTraceCollector implements IRewriterTraceCollector {

    public static final IRewriterTraceCollector INSTANCE = new NopTraceCollector();

    private NopTraceCollector() {
        // Private constructor to force using the common instance
    }

    /**
     * @since 2.0
     */
    @Override
    public Stream<PTraceable> getCanonicalTraceables(PTraceable derivative) {
        return Stream.empty();
    }

    /**
     * @since 2.0
     */
    @Override
    public Stream<PTraceable> getRewrittenTraceables(PTraceable source) {
        return Stream.empty();
    }


    @Override
    public void addTrace(PTraceable origin, PTraceable derivative) {
        // ignored
    }

    @Override
    public void derivativeRemoved(PTraceable derivative, IDerivativeModificationReason reason) {
        // ignored
    }

    @Override
    public boolean isRemoved(PTraceable traceable) {
        return false;
    }

    /**
     * @since 2.0
     */
    @Override
    public Stream<IDerivativeModificationReason> getRemovalReasons(PTraceable traceable) {
        return Stream.empty();
    }

}
