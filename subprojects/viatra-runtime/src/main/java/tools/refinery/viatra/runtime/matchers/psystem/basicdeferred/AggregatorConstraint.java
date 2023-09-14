/*******************************************************************************
 * Copyright (c) 2010-2016, Tamas Szabo, Istvan Rath and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.viatra.runtime.matchers.psystem.basicdeferred;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import tools.refinery.viatra.runtime.matchers.context.IInputKey;
import tools.refinery.viatra.runtime.matchers.context.IQueryMetaContext;
import tools.refinery.viatra.runtime.matchers.psystem.ITypeInfoProviderConstraint;
import tools.refinery.viatra.runtime.matchers.psystem.PBody;
import tools.refinery.viatra.runtime.matchers.psystem.PVariable;
import tools.refinery.viatra.runtime.matchers.psystem.TypeJudgement;
import tools.refinery.viatra.runtime.matchers.psystem.aggregations.BoundAggregator;
import tools.refinery.viatra.runtime.matchers.psystem.queries.PQuery;
import tools.refinery.viatra.runtime.matchers.tuple.Tuple;
import tools.refinery.viatra.runtime.matchers.tuple.Tuples;

/**
 * The PSystem representation of an aggregation.
 * 
 * @author Tamas Szabo
 * @since 1.4
 */
public class AggregatorConstraint extends PatternCallBasedDeferred implements ITypeInfoProviderConstraint {

    protected PVariable resultVariable;
    private BoundAggregator aggregator;
    protected int aggregatedColumn;

    public AggregatorConstraint(BoundAggregator aggregator, PBody pBody, Tuple actualParametersTuple, PQuery query,
            PVariable resultVariable, int aggregatedColumn) {
        super(pBody, actualParametersTuple, query, Collections.singleton(resultVariable));
        this.resultVariable = resultVariable;
        this.aggregatedColumn = aggregatedColumn;
        this.aggregator = aggregator;
    }

    public int getAggregatedColumn() {
        return this.aggregatedColumn;
    }

    public BoundAggregator getAggregator() {
        return this.aggregator;
    }

    @Override
    public Set<PVariable> getDeducedVariables() {
        return Collections.singleton(resultVariable);
    }

    @Override
    public Map<Set<PVariable>, Set<PVariable>> getFunctionalDependencies(IQueryMetaContext context) {
        final Map<Set<PVariable>, Set<PVariable>> result = new HashMap<Set<PVariable>, Set<PVariable>>();
        result.put(getDeferringVariables(), getDeducedVariables());
        return result;
    }

    @Override
    protected void doDoReplaceVariables(PVariable obsolete, PVariable replacement) {
        if (resultVariable.equals(obsolete))
            resultVariable = replacement;
    }

    @Override
    protected Set<PVariable> getCandidateQuantifiedVariables() {
        return actualParametersTuple.<PVariable> getDistinctElements();
    }

    @Override
    protected String toStringRest() {
        return query.getFullyQualifiedName() + "@" + actualParametersTuple.toString() + "->"
                + resultVariable.toString();
    }

    public PVariable getResultVariable() {
        return resultVariable;
    }

    @Override
    public Set<TypeJudgement> getImpliedJudgements(IQueryMetaContext context) {
        Set<TypeJudgement> result = new HashSet<TypeJudgement>();
        IInputKey aggregateResultType = aggregator.getAggregateResultTypeAsInputKey();
        if (aggregateResultType != null) {
            result.add(new TypeJudgement(aggregateResultType, Tuples.staticArityFlatTupleOf(resultVariable)));
        }
        return result;
    }
}
