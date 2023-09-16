/*******************************************************************************
 * Copyright (c) 2004-2010 Gabor Bergmann and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package tools.refinery.interpreter.matchers.psystem.basicdeferred;

import java.util.Collections;
import java.util.Set;

import tools.refinery.interpreter.matchers.psystem.PBody;
import tools.refinery.interpreter.matchers.psystem.PVariable;
import tools.refinery.interpreter.matchers.psystem.queries.PQuery;
import tools.refinery.interpreter.matchers.tuple.Tuple;

/**
 * @author Gabor Bergmann
 *
 */
public class NegativePatternCall extends PatternCallBasedDeferred {

    public NegativePatternCall(PBody pBody, Tuple actualParametersTuple, PQuery query) {
        super(pBody, actualParametersTuple, query);
    }

    @Override
    public Set<PVariable> getDeducedVariables() {
        return Collections.emptySet();
    }

    /**
     * @return all variables that may potentially be quantified they are not used anywhere else
     */
    @Override
    protected Set<PVariable> getCandidateQuantifiedVariables() {
        return getAffectedVariables();
    }

    @Override
    protected void doDoReplaceVariables(PVariable obsolete, PVariable replacement) {
    }

    @Override
    protected String toStringRest() {
        return "!" + query.getFullyQualifiedName() + "@" + actualParametersTuple.toString();
    }

}
