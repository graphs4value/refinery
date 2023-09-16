/*******************************************************************************
 * Copyright (c) 2010-2018, Gabor Bergmann, IncQuery Labs Ltd.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.interpreter.matchers.backend;

import tools.refinery.interpreter.matchers.context.IQueryResultProviderAccess;
import tools.refinery.interpreter.matchers.psystem.IQueryReference;
import tools.refinery.interpreter.matchers.psystem.PConstraint;

/**
 * Uniform way of requesting result providers for pattern calls within queries.
 * Intended users are query backends, for calling other backends to deliver results of dependee queries.
 *
 * @author Gabor Bergmann
 * @since 2.1
 */
public class ResultProviderRequestor {
    IQueryBackend callerBackend;
    IQueryResultProviderAccess resultProviderAccess;
    IQueryBackendHintProvider hintProvider;
    ICallDelegationStrategy delegationStrategy;
    QueryEvaluationHint callerHint;
    QueryEvaluationHint universalOverride;


    /**
     * @param callerBackend the actual backend evaluating the calling pattern.
     * @param resultProviderAccess
     * @param hintProvider
     * @param delegationStrategy
     * @param callerHint  a hint under which the calling pattern is evaluated,
     * @param universalOverride if non-null, overrides the hint with extra options <i>after</i> the {@link ICallDelegationStrategy}
     */
    public ResultProviderRequestor(IQueryBackend callerBackend, IQueryResultProviderAccess resultProviderAccess,
            IQueryBackendHintProvider hintProvider, ICallDelegationStrategy delegationStrategy,
            QueryEvaluationHint callerHint, QueryEvaluationHint universalOverride) {
        super();
        this.callerBackend = callerBackend;
        this.resultProviderAccess = resultProviderAccess;
        this.hintProvider = hintProvider;
        this.delegationStrategy = delegationStrategy;
        this.callerHint = callerHint;
        this.universalOverride = universalOverride;
    }




    /**
     *
     * @param call a {@link PConstraint} in a query that calls another query.
     * @param spotOverride if non-null, overrides the hint with extra options <i>after</i> the {@link ICallDelegationStrategy}
     * and the universal override specified in the constructor
     * @return the obtained result provider
     */
    public IQueryResultProvider requestResultProvider(IQueryReference call, QueryEvaluationHint spotOverride) {
        QueryEvaluationHint hints =
                delegationStrategy.transformHints(call, callerHint, callerBackend, hintProvider);

        if (universalOverride != null)
            hints = hints.overrideBy(universalOverride);

        if (spotOverride != null)
            hints = hints.overrideBy(spotOverride);

        return resultProviderAccess.getResultProvider(call.getReferredQuery(), hints);
    }

}
