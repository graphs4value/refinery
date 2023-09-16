/*******************************************************************************
 * Copyright (c) 2010-2015, Zoltan Ujhelyi, Marton Bur, Istvan Rath and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.interpreter.localsearch.matcher;

import java.util.Set;

import tools.refinery.interpreter.matchers.backend.QueryEvaluationHint;
import tools.refinery.interpreter.matchers.psystem.queries.PParameter;
import tools.refinery.interpreter.matchers.psystem.queries.PQuery;

public class MatcherReference {
    final PQuery query;
    final Set<PParameter> adornment;

    /**
     * Hints that can override the callee's own hints. This field is intentionally left out from hashCode and equals
     */
    final QueryEvaluationHint hints;

    /**
     * @since 1.4
     */
    public MatcherReference(PQuery query, Set<PParameter> adornment, QueryEvaluationHint hints) {
        super();
        this.query = query;
        this.adornment = adornment;
        this.hints = hints;
    }

    public MatcherReference(PQuery query, Set<PParameter> adornment){
        this(query, adornment, null);
    }

    public PQuery getQuery() {
        return query;
    }
    public Set<PParameter> getAdornment() {
        return adornment;
    }
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;

        result = prime * result + ((adornment == null) ? 0 : adornment.hashCode());
        result = prime * result + ((query == null) ? 0 : query.hashCode());
        return result;
    }
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        MatcherReference other = (MatcherReference) obj;
        if (adornment == null) {
            if (other.adornment != null)
                return false;
        } else if (!adornment.equals(other.adornment))
            return false;
        if (query == null) {
            if (other.query != null)
                return false;
        } else if (!query.equals(other.query))
            return false;
        return true;
    }

    /**
     * @return the hints to override the called reference's own hints with. Can be null.
     * @since 1.4
     */
    public QueryEvaluationHint getHints() {
        return hints;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(query.getFullyQualifiedName());
        sb.append("(");
        for(PParameter p : query.getParameters()){
            sb.append(adornment.contains(p) ? "b" : "f");
        }
        sb.append(")");
        return sb.toString();
    }

}
