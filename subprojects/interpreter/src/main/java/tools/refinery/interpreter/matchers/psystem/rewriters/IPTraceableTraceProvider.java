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
import tools.refinery.interpreter.matchers.psystem.queries.PQuery;

/**
 * This interface provides methods to trace the {@link PTraceable}s of a transformed {@link PQuery} produced by
 * a {@link PDisjunctionRewriter}. In case the associated rewriter is a composite (a.k.a. {@link PDisjunctionRewriterCacher}),
 * this trace provider handles traces end-to-end, hiding all the intermediate transformation steps.
 *
 * @since 1.6
 * @noimplement This interface is not intended to be implemented by clients.
 */
public interface IPTraceableTraceProvider {

    /**
     * Find and return the canonical {@link PTraceable}s in the original query which are the sources of the given derivative
     * {@link PTraceable} according to the transformation.
     *
     * @param derivative a {@link PTraceable} which is contained by the {@link PQuery} produced by the associated rewriter
     * @since 2.0
     */
    public Stream<PTraceable> getCanonicalTraceables(PTraceable derivative);

    /**
     * Find and return the {@link PTraceable}s in the rewritten query which are the destinations of the given source
     * {@link PTraceable} according to the transformation.
     *
     * @param source a {@link PTraceable} which is contained by a {@link PQuery} before rewriting
     * @since 2.0
     */
    public Stream<PTraceable> getRewrittenTraceables(PTraceable source);

    /**
     * Returns whether the given traceable element has been removed by every rewriter for a reason.
     */
    public boolean isRemoved(PTraceable traceable);

    /**
     * Returns the reasons for which the traceable element has been removed by the rewriters.
     * @return the reasons of removal during rewriting
     * @since 2.0
     */
    public Stream<IDerivativeModificationReason> getRemovalReasons(PTraceable traceable);
}
