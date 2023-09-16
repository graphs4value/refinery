/*******************************************************************************
 * Copyright (c) 2004-2010 Gabor Bergmann and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package tools.refinery.interpreter.matchers.psystem.basicenumerables;

import java.util.HashSet;
import java.util.Set;

import tools.refinery.interpreter.matchers.context.IInputKey;
import tools.refinery.interpreter.matchers.context.IQueryMetaContext;
import tools.refinery.interpreter.matchers.psystem.IQueryReference;
import tools.refinery.interpreter.matchers.psystem.ITypeInfoProviderConstraint;
import tools.refinery.interpreter.matchers.psystem.KeyedEnumerablePConstraint;
import tools.refinery.interpreter.matchers.psystem.PBody;
import tools.refinery.interpreter.matchers.psystem.TypeJudgement;
import tools.refinery.interpreter.matchers.psystem.queries.PQuery;
import tools.refinery.interpreter.matchers.tuple.Tuple;
import tools.refinery.interpreter.matchers.tuple.Tuples;

/**
 * @author Gabor Bergmann
 *
 */
public class PositivePatternCall extends KeyedEnumerablePConstraint<PQuery> implements IQueryReference, ITypeInfoProviderConstraint {

    public PositivePatternCall(PBody pBody, Tuple variablesTuple,
            PQuery pattern) {
        super(pBody, variablesTuple, pattern);
    }

    @Override
    protected String keyToString() {
        return supplierKey.getFullyQualifiedName();
    }

    // Note: #getFunctionalDependencies is intentionally not implemented - use QueryAnalyzer instead!
//    @Override
//    public Map<Set<PVariable>, Set<PVariable>> getFunctionalDependencies(IQueryMetaContext context) {
//        return super.getFunctionalDependencies(context);
//    }

    @Override
    public PQuery getReferredQuery() {
        return supplierKey;
    }

    @Override
    public Set<TypeJudgement> getImpliedJudgements(IQueryMetaContext context) {
        return getTypesImpliedByCall(supplierKey, variablesTuple);
    }

    /**
     * @since 1.3
     */
    public static Set<TypeJudgement> getTypesImpliedByCall(PQuery calledQuery, Tuple actualParametersTuple) {
        Set<TypeJudgement> result = new HashSet<TypeJudgement>();
        for (TypeJudgement parameterJudgement : calledQuery.getTypeGuarantees()) {
            IInputKey inputKey = parameterJudgement.getInputKey();
            Tuple judgementIndexTuple = parameterJudgement.getVariablesTuple();

            Object[] judgementVariables = new Object[judgementIndexTuple.getSize()];
            for (int i=0; i<judgementVariables.length; ++i)
                judgementVariables[i] = actualParametersTuple.get((int) judgementIndexTuple.get(i));

            result.add(new TypeJudgement(inputKey, Tuples.flatTupleOf(judgementVariables)));
        }
        return result;
    }

}
