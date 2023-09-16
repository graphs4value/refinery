/*******************************************************************************
 * Copyright (c) 2010-2016, Grill Balázs, IncQueryLabs Ltd.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.interpreter.localsearch.matcher.integration;

import java.util.Set;
import java.util.stream.Collectors;

import tools.refinery.interpreter.matchers.psystem.queries.PParameter;
import tools.refinery.interpreter.matchers.psystem.queries.PParameterDirection;
import tools.refinery.interpreter.matchers.psystem.queries.PQueries;
import tools.refinery.interpreter.matchers.psystem.queries.PQuery;
import tools.refinery.interpreter.matchers.util.Sets;


/**
 * This implementation calculates all valid adornments for the given query, respecting the parameter direction constraints.
 *
 * @author Grill Balázs
 * @since 1.5
 */
public class AllValidAdornments implements IAdornmentProvider {

    @Override
    public Iterable<Set<PParameter>> getAdornments(PQuery query) {
        final Set<PParameter> ins = query.getParameters().stream().filter(PQueries.parameterDirectionPredicate(PParameterDirection.IN)).collect(Collectors.toSet());
        Set<PParameter> inouts = query.getParameters().stream().filter(PQueries.parameterDirectionPredicate(PParameterDirection.INOUT)).collect(Collectors.toSet());
        Set<? extends Set<PParameter>> possibleInouts = Sets.powerSet(inouts);
        return possibleInouts.stream().map(input -> Sets.union(ins, input)).collect(Collectors.toSet());
    }

}
