/*******************************************************************************
 * Copyright (c) 2010-2015, Marton Bur, Zoltan Ujhelyi, Istvan Rath and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.interpreter.localsearch.matcher.integration;

import tools.refinery.interpreter.matchers.backend.IMatcherCapability;
import tools.refinery.interpreter.matchers.backend.IQueryBackend;
import tools.refinery.interpreter.matchers.backend.IQueryBackendFactory;
import tools.refinery.interpreter.matchers.backend.QueryEvaluationHint;
import tools.refinery.interpreter.matchers.context.IQueryBackendContext;
import tools.refinery.interpreter.matchers.psystem.queries.PQuery;

/**
 * @author Marton Bur, Zoltan Ujhelyi
 * @since 1.7
 *
 */
public enum LocalSearchGenericBackendFactory implements IQueryBackendFactory {

    INSTANCE;

    /**
     * @since 1.5
     */
    @Override
    public IQueryBackend create(IQueryBackendContext context) {
        return new LocalSearchBackend(context) {

            @Override
            protected AbstractLocalSearchResultProvider initializeResultProvider(PQuery query, QueryEvaluationHint hints) {
                return new GenericLocalSearchResultProvider(this, context, query, planProvider, hints);
            }

            @Override
            public IQueryBackendFactory getFactory() {
                return INSTANCE;
            }

        };
    }

    @Override
    public Class<? extends IQueryBackend> getBackendClass() {
        return LocalSearchBackend.class;
    }

    /**
     * @since 1.4
     */
    @Override
    public IMatcherCapability calculateRequiredCapability(PQuery query, QueryEvaluationHint hint) {
        return LocalSearchHints.parse(hint);
    }

    @Override
    public boolean isCaching() {
        return false;
    }

}
