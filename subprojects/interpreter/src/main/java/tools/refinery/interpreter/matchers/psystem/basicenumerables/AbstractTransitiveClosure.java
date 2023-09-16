/*******************************************************************************
 * Copyright (c) 2010-2018, Gabor Bergmann, Zoltan Ujhelyi, IncQuery Labs Ltd.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.interpreter.matchers.psystem.basicenumerables;

import java.util.Set;

import tools.refinery.interpreter.matchers.context.IQueryMetaContext;
import tools.refinery.interpreter.matchers.psystem.IQueryReference;
import tools.refinery.interpreter.matchers.psystem.ITypeInfoProviderConstraint;
import tools.refinery.interpreter.matchers.psystem.KeyedEnumerablePConstraint;
import tools.refinery.interpreter.matchers.psystem.PBody;
import tools.refinery.interpreter.matchers.psystem.TypeJudgement;
import tools.refinery.interpreter.matchers.psystem.queries.PQuery;
import tools.refinery.interpreter.matchers.tuple.Tuple;

/**
 * @since 2.0
 */
public abstract class AbstractTransitiveClosure extends KeyedEnumerablePConstraint<PQuery> implements IQueryReference, ITypeInfoProviderConstraint {

    public AbstractTransitiveClosure(PBody pBody, Tuple variablesTuple, PQuery supplierKey) {
        super(pBody, variablesTuple, supplierKey);
    }

    @Override
    public PQuery getReferredQuery() {
        return supplierKey;
    }

    /**
     * @since 1.3
     */
    @Override
    public Set<TypeJudgement> getImpliedJudgements(IQueryMetaContext context) {
        return PositivePatternCall.getTypesImpliedByCall(supplierKey, variablesTuple);
    }

}
