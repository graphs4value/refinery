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
 * Function object that specifies how hints (including backend preferences) shall propagate through pattern calls.
 *
 * <p> A few useful default implementations are included as static fields.
 *
 * <p> As of 2.1, only suppported by the local search backend, and only if the pattern call is not flattened.
 *
 * @author Gabor Bergmann
 * @since 2.1
 */
@FunctionalInterface
public interface ICallDelegationStrategy {

    /**
     * Specifies how hints (including backend preferences) shall propagate through pattern calls.
     *
     * @param call a {@link PConstraint} in a query that calls another query.
     * @param callerHint  a hint under which the calling pattern is evaluated,
     * @param callerBackend the actual backend evaluating the calling pattern.
     * @param calleeHintProvider the provider of hints for the called pattern.
     * @return the hints, including backend selection,
     * that the backend responsible for the caller pattern must specify when
     * requesting the {@link IQueryResultProvider} for the called pattern via {@link IQueryResultProviderAccess}.
     */
    public QueryEvaluationHint transformHints(IQueryReference call,
                                              QueryEvaluationHint callerHint,
                                              IQueryBackend callerBackend,
                                              IQueryBackendHintProvider calleeHintProvider);


    /**
     * Options known for callee are used to override caller options, except the backend selection.
     * Always use the same backend for the callee and the caller, regardless what is specified for the callee pattern.
     * @author Gabor Bergmann
     */
    public static final ICallDelegationStrategy FULL_BACKEND_ADHESION = (call, callerHint, callerBackend, calleeHintProvider) -> {
        QueryEvaluationHint calleeHint =
                calleeHintProvider.getQueryEvaluationHint(call.getReferredQuery());
        QueryEvaluationHint result =
            callerHint == null ? calleeHint : callerHint.overrideBy(calleeHint);

        QueryEvaluationHint backendAdhesion = new QueryEvaluationHint(
                null /* settings-ignorant */, callerBackend.getFactory());
        result = result.overrideBy(backendAdhesion);
        return result;
    };
    /**
     * Options known for callee are used to override caller options, including the backend selection.
     * If callee does not specify a backend requirement, the backend of the caller is kept.
     * @author Gabor Bergmann
     */
    public static final ICallDelegationStrategy PARTIAL_BACKEND_ADHESION = (call, callerHint, callerBackend, calleeHintProvider) -> {
        QueryEvaluationHint backendAdhesion = new QueryEvaluationHint(
                null /* settings-ignorant */, callerBackend.getFactory());

        QueryEvaluationHint result =
                callerHint == null ? backendAdhesion : callerHint.overrideBy(backendAdhesion);

        QueryEvaluationHint calleeHint = calleeHintProvider.getQueryEvaluationHint(call.getReferredQuery());
        result = result.overrideBy(calleeHint);

        return result;
    };
    /**
     * Options known for callee are used to override caller options, including the backend selection.
     * Always use the backend specified for the callee (or the default if none), regardless of the backend of the caller.
     * @author Gabor Bergmann
     */
    public static final ICallDelegationStrategy NO_BACKEND_ADHESION = (call, callerHint, callerBackend, calleeHintProvider) -> {
        QueryEvaluationHint calleeHint = calleeHintProvider.getQueryEvaluationHint(call.getReferredQuery());
        return callerHint == null ? calleeHint : callerHint.overrideBy(calleeHint);
    };


}
