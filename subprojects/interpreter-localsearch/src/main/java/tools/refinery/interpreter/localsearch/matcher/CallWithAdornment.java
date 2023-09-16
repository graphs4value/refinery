/*******************************************************************************
 * Copyright (c) 2010-2018, Gabor Bergmann, IncQuery Labs Ltd.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.interpreter.localsearch.matcher;

import java.util.HashSet;
import java.util.Set;

import tools.refinery.interpreter.matchers.psystem.IQueryReference;
import tools.refinery.interpreter.matchers.psystem.PConstraint;
import tools.refinery.interpreter.matchers.psystem.queries.PParameter;
import tools.refinery.interpreter.matchers.psystem.queries.PQuery;

/**
 * Immutable data that represents the role of a pattern call within an LS query plan.
 *
 * <p> The call is expressed as the {@link PConstraint} {@link #call} (implementing {@link IQueryReference}),
 * while the stored {@link #adornment} records the way it will be used within a search plan (specifically,
 * pattern parameters within the adornment will have their values known at the point of evaluating the constraint).
 *
 *
 * @author Gabor Bergmann
 * @since 2.1
 */
public class CallWithAdornment {
    private final IQueryReference call;
    private final Set<PParameter> adornment;

    public CallWithAdornment(IQueryReference call, Set<PParameter> adornment) {
        this.call = call;
        this.adornment = new HashSet<>(adornment);
    }

    public IQueryReference getCall() {
        return call;
    }

    public Set<PParameter> getAdornment() {
        return adornment;
    }


    public PQuery getReferredQuery() {
        return call.getReferredQuery();
    }

    public MatcherReference getMatcherReference() {
        return new MatcherReference(getReferredQuery(), adornment);
    }
}
