/*******************************************************************************
 * Copyright (c) 2010-2016, Grill Balázs, IncQueryLabs Ltd.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.interpreter.localsearch.matcher.integration;

import tools.refinery.interpreter.matchers.backend.QueryEvaluationHint;
import tools.refinery.interpreter.matchers.backend.QueryEvaluationHint.BackendRequirement;
import tools.refinery.interpreter.matchers.psystem.basicenumerables.PositivePatternCall;
import tools.refinery.interpreter.matchers.psystem.rewriters.IFlattenCallPredicate;

/**
 * This implementation forbids flattening of patterns marked to be executed with a caching / incremental backend.
 * This makes is possible for the user to configure hybrid matching via using
 * the 'search' and 'incremental keywords in the pattern definition file.
 *
 * @since 1.5
 *
 */
public class DontFlattenIncrementalPredicate implements IFlattenCallPredicate {

    @Override
    public boolean shouldFlatten(PositivePatternCall positivePatternCall) {
        QueryEvaluationHint evaluationHints = positivePatternCall.getReferredQuery().getEvaluationHints();
        if (evaluationHints == null) return true;

        BackendRequirement backendRequirementType = evaluationHints.getQueryBackendRequirementType();
        switch(backendRequirementType) {
        case DEFAULT_CACHING:
            return false;
        case SPECIFIC:
            return !evaluationHints.getQueryBackendFactory().isCaching();
        case UNSPECIFIED:
        case DEFAULT_SEARCH:
        default:
            return true;
        }
    }

}
