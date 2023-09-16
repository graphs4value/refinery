/*******************************************************************************
 * Copyright (c) 2010-2018, Gabor Bergmann, IncQuery Labs Ltd.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.interpreter.localsearch.matcher.integration;

import java.util.Collections;
import java.util.Set;

import tools.refinery.interpreter.matchers.psystem.queries.PParameter;
import tools.refinery.interpreter.matchers.psystem.queries.PQuery;

/**
 * This adornment provider does not trigger the preparation of any plans.
 * Actual query plans will be computed on demand, when the first actual match request is made with a given adornment.
 *
 * <p> Caution: this is a safe default adornment provider for {@link GenericLocalSearchResultProvider} only;
 * do not use for the EMF-specific LS backend.
 *
 * <p> The benefits is in execution time: query planning costs for adornments are postponed until first usage
 * or even entirely avoided (when adornment is never used in practice).
 * However, query evaluation time may become less predictable, as the first matcher call (with a given adornment)
 * will include the planning cost.
 * For benchmarking or other purposes where this is not desirable, use an adornment provider that demands plan precomputation for all necessary adornments.
 *
 * @author Gabor Bergmann
 * @since 2.1
 *
 */
public class LazyPlanningAdornments implements IAdornmentProvider {

    @Override
    public Iterable<Set<PParameter>> getAdornments(PQuery query) {
        return Collections.emptySet();
    }

}
