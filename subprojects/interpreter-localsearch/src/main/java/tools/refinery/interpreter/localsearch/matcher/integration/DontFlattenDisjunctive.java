/*******************************************************************************
 * Copyright (c) 2010-2018, Gabor Bergmann, IncQuery Labs Ltd.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.interpreter.localsearch.matcher.integration;

import tools.refinery.interpreter.matchers.psystem.basicenumerables.PositivePatternCall;
import tools.refinery.interpreter.matchers.psystem.rewriters.IFlattenCallPredicate;

/**
 * Forbids flattening of patterns that have more than one body.
 *
 * @since 2.1

 * @author Gabor Bergmann
 *
 */
public class DontFlattenDisjunctive implements IFlattenCallPredicate {

    @Override
    public boolean shouldFlatten(PositivePatternCall positivePatternCall) {
        return 1 >= positivePatternCall.getReferredQuery().getDisjunctBodies().getBodies().size();
    }

}
