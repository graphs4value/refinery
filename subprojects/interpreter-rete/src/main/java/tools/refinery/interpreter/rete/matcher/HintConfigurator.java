/*******************************************************************************
 * Copyright (c) 2010-2016, Gabor Bergmann, IncQueryLabs Ltd.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.interpreter.rete.matcher;

import java.util.HashMap;
import java.util.Map;

import tools.refinery.interpreter.matchers.backend.IQueryBackendHintProvider;
import tools.refinery.interpreter.matchers.backend.QueryEvaluationHint;
import tools.refinery.interpreter.matchers.psystem.queries.PQuery;

/**
 * A configurable hint provider that gathers hints for queries during runtime, and delegates defaults to an external hint provider.
 *
 * @author Gabor Bergmann
 * @since 1.5
 */
class HintConfigurator implements IQueryBackendHintProvider {

    private IQueryBackendHintProvider defaultHintProvider;
    private Map<PQuery, QueryEvaluationHint> storedHints = new HashMap<PQuery, QueryEvaluationHint>();

    public HintConfigurator(IQueryBackendHintProvider defaultHintProvider) {
        this.defaultHintProvider = defaultHintProvider;
    }

    @Override
    public QueryEvaluationHint getQueryEvaluationHint(PQuery query) {
        return defaultHintProvider.getQueryEvaluationHint(query).overrideBy(storedHints.get(query));
    }

    public void storeHint(PQuery query, QueryEvaluationHint hint) {
        QueryEvaluationHint oldHint = storedHints.get(query);
        if (oldHint == null)
            storedHints.put(query, hint);
        else
            storedHints.put(query, oldHint.overrideBy(hint));
    }

}
