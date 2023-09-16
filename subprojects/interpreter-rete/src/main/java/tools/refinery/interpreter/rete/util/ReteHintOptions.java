/*******************************************************************************
 * Copyright (c) 2010-2016, Gabor Bergmann, IncQueryLabs Ltd.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.interpreter.rete.util;

import tools.refinery.interpreter.rete.matcher.DRedReteBackendFactory;
import tools.refinery.interpreter.matchers.backend.QueryEvaluationHint;
import tools.refinery.interpreter.matchers.backend.QueryHintOption;

/**
 * Provides key objects (of type {@link QueryHintOption}) for {@link QueryEvaluationHint}s.
 * @author Gabor Bergmann
 * @since 1.5
 */
public final class ReteHintOptions {

    private ReteHintOptions() {/*Utility class constructor*/}

    public static final QueryHintOption<Boolean> useDiscriminatorDispatchersForConstantFiltering =
            hintOption("useDiscriminatorDispatchersForConstantFiltering", true);

    public static final QueryHintOption<Boolean> prioritizeConstantFiltering =
            hintOption("prioritizeConstantFiltering", true);

    public static final QueryHintOption<Boolean> cacheOutputOfEvaluatorsByDefault =
            hintOption("cacheOutputOfEvaluatorsByDefault", true);

    /**
     * The incremental query evaluator backend can evaluate recursive patterns.
     * However, by default, instance models that contain cycles are not supported with recursive queries
     * and can lead to incorrect query results.
     * Enabling Delete And Rederive (DRED) mode guarantees that recursive query evaluation leads to correct results in these cases as well.
     *
     * <p> As DRED may diminish the performance of incremental maintenance, it is not enabled by default.
     * @since 1.6
     * @deprecated Use {@link DRedReteBackendFactory} instead of setting this option to true.
     */
    @Deprecated
    public static final QueryHintOption<Boolean> deleteRederiveEvaluation =
            hintOption("deleteRederiveEvaluation", false);

    /**
     * This hint allows the query planner to take advantage of "weakened alternative" suggestions of the meta context.
     * For instance, enumerable unary type constraints may be substituted with a simple type filtering where sufficient.
     *
     * @since 1.6
     */
    public static final QueryHintOption<Boolean> expandWeakenedAlternativeConstraints =
            hintOption("expandWeakenedAlternativeConstraints", true);

    // internal helper for conciseness
    private static <T> QueryHintOption<T> hintOption(String hintKeyLocalName, T defaultValue) {
        return new QueryHintOption<>(ReteHintOptions.class, hintKeyLocalName, defaultValue);
    }
}
