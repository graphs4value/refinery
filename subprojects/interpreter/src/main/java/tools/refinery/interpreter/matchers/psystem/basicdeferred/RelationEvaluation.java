/*******************************************************************************
 * Copyright (c) 2010-2022, Tamas Szabo, GitHub
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.interpreter.matchers.psystem.basicdeferred;

import java.util.List;

import tools.refinery.interpreter.matchers.psystem.EnumerablePConstraint;
import tools.refinery.interpreter.matchers.psystem.IMultiQueryReference;
import tools.refinery.interpreter.matchers.psystem.IRelationEvaluator;
import tools.refinery.interpreter.matchers.psystem.PBody;
import tools.refinery.interpreter.matchers.psystem.queries.PQuery;
import tools.refinery.interpreter.matchers.tuple.Tuple;

/**
 * A constraint which prescribes the evaluation of custom Java logic that takes an arbitrary number of input relations
 * and produces one output relation. Contrast this to {@link ExpressionEvaluation}, which produces a single output value
 * given an input tuple.
 *
 * The assumption is that the relation evaluation logic is not incremental, that is, it can only perform from-scratch
 * computation of the output relation given the complete input relations. To this end, the relation evaluator always
 * receives the complete input relations with all their contents as input. However, the evaluator engine makes sure that
 * the output of the relation evaluation is at least "seemingly" incremental. This means that the underlying computation
 * network computes the delta on the output compared to the previous output and only propagates the delta further.
 *
 * @author Tamas Szabo
 *
 * @since 2.8
 *
 */
public class RelationEvaluation extends EnumerablePConstraint implements IMultiQueryReference {

    private final IRelationEvaluator evaluator;
    private final List<PQuery> inputQueries;

    public RelationEvaluation(final PBody body, final Tuple variablesTuple, final List<PQuery> inputQueries,
            final IRelationEvaluator evaluator) {
        super(body, variablesTuple);
        this.evaluator = evaluator;
        this.inputQueries = inputQueries;
    }

    public IRelationEvaluator getEvaluator() {
        return this.evaluator;
    }

    @Override
    public List<PQuery> getReferredQueries() {
        return this.inputQueries;
    }

}
