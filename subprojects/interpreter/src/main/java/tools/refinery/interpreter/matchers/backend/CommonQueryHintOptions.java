/*******************************************************************************
 * Copyright (c) 2010-2017, Grill Balázs, IncQueryLabs
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.interpreter.matchers.backend;

import tools.refinery.interpreter.matchers.psystem.rewriters.IRewriterTraceCollector;
import tools.refinery.interpreter.matchers.psystem.rewriters.NopTraceCollector;

/**
 * Query evaluation hints applicable to any engine
 * @since 1.6
 *
 */
public final class CommonQueryHintOptions {

    private CommonQueryHintOptions() {
        // Hiding constructor for utility class
    }

    /**
     * This hint instructs the query backends to record trace information into the given trace collector
     */
    public static final QueryHintOption<IRewriterTraceCollector> normalizationTraceCollector =
            hintOption("normalizationTraceCollector", NopTraceCollector.INSTANCE);

    // internal helper for conciseness
    private static <T> QueryHintOption<T> hintOption(String hintKeyLocalName, T defaultValue) {
        return new QueryHintOption<>(CommonQueryHintOptions.class, hintKeyLocalName, defaultValue);
    }

}
