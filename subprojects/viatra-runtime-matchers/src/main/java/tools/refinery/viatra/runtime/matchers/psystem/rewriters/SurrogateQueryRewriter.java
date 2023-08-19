/*******************************************************************************
 * Copyright (c) 2010-2015, Zoltan Ujhelyi, Istvan Rath and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.viatra.runtime.matchers.psystem.rewriters;

import java.util.LinkedHashSet;
import java.util.Set;

import tools.refinery.viatra.runtime.matchers.context.IInputKey;
import tools.refinery.viatra.runtime.matchers.context.surrogate.SurrogateQueryRegistry;
import tools.refinery.viatra.runtime.matchers.psystem.PBody;
import tools.refinery.viatra.runtime.matchers.psystem.PVariable;
import tools.refinery.viatra.runtime.matchers.psystem.basicenumerables.PositivePatternCall;
import tools.refinery.viatra.runtime.matchers.psystem.basicenumerables.TypeConstraint;
import tools.refinery.viatra.runtime.matchers.psystem.queries.PDisjunction;
import tools.refinery.viatra.runtime.matchers.psystem.queries.PQuery;
import tools.refinery.viatra.runtime.matchers.psystem.queries.PQuery.PQueryStatus;
import tools.refinery.viatra.runtime.matchers.tuple.Tuple;
import tools.refinery.viatra.runtime.matchers.tuple.Tuples;

/**
 * @author Zoltan Ujhelyi
 *
 */
public class SurrogateQueryRewriter extends PDisjunctionRewriter {

    @Override
    public PDisjunction rewrite(PDisjunction disjunction) {
        Set<PBody> replacedBodies = new LinkedHashSet<>();
        for (PBody body : disjunction.getBodies()) {
            PBodyCopier copier = new PBodyCopier(body, getTraceCollector()) {
            	
            	@Override
            	protected void copyTypeConstraint(TypeConstraint typeConstraint) {
                    PVariable[] mappedVariables = extractMappedVariables(typeConstraint);
                    Tuple variablesTuple = Tuples.flatTupleOf((Object[])mappedVariables); 	
                    final IInputKey supplierKey = typeConstraint.getSupplierKey();
                    if(SurrogateQueryRegistry.instance().hasSurrogateQueryFQN(supplierKey)) {
                        PQuery surrogateQuery = SurrogateQueryRegistry.instance().getSurrogateQuery(supplierKey);
                        if (surrogateQuery == null) {
                            throw new IllegalStateException(
                            		String.format("Surrogate query for feature %s not found", 
                            				supplierKey.getPrettyPrintableName()));
                        }
                        addTrace(typeConstraint, new PositivePatternCall(getCopiedBody(), variablesTuple, surrogateQuery));
                    } else {
                    	addTrace(typeConstraint, new TypeConstraint(getCopiedBody(), variablesTuple, supplierKey));
                    }
            	}
            };
            PBody modifiedBody = copier.getCopiedBody();
            replacedBodies.add(modifiedBody);
            modifiedBody.setStatus(PQueryStatus.OK);
        }
        return new PDisjunction(replacedBodies);
    }

}
