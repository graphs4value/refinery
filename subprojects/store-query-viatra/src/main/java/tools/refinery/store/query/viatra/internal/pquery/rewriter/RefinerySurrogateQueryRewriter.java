/*******************************************************************************
 * Copyright (c) 2010-2015, Zoltan Ujhelyi, Istvan Rath and Daniel Varro
 * Copyright (c) 2023 The Refinery Authors <https://refinery.tools/>
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.store.query.viatra.internal.pquery.rewriter;

import org.eclipse.viatra.query.runtime.matchers.context.IInputKey;
import org.eclipse.viatra.query.runtime.matchers.context.surrogate.SurrogateQueryRegistry;
import org.eclipse.viatra.query.runtime.matchers.psystem.PBody;
import org.eclipse.viatra.query.runtime.matchers.psystem.PVariable;
import org.eclipse.viatra.query.runtime.matchers.psystem.basicenumerables.PositivePatternCall;
import org.eclipse.viatra.query.runtime.matchers.psystem.basicenumerables.TypeConstraint;
import org.eclipse.viatra.query.runtime.matchers.psystem.queries.PDisjunction;
import org.eclipse.viatra.query.runtime.matchers.psystem.queries.PQuery;
import org.eclipse.viatra.query.runtime.matchers.psystem.rewriters.PBodyCopier;
import org.eclipse.viatra.query.runtime.matchers.psystem.rewriters.PDisjunctionRewriter;
import org.eclipse.viatra.query.runtime.matchers.tuple.Tuple;
import org.eclipse.viatra.query.runtime.matchers.tuple.Tuples;

import java.util.LinkedHashSet;
import java.util.Set;

public class RefinerySurrogateQueryRewriter extends PDisjunctionRewriter {
	@Override
	public PDisjunction rewrite(PDisjunction disjunction) {
		Set<PBody> replacedBodies = new LinkedHashSet<>();
		for (PBody body : disjunction.getBodies()) {
			PBodyCopier copier = new RefineryPBodyCopier(body, getTraceCollector()) {

				@Override
				protected void copyTypeConstraint(TypeConstraint typeConstraint) {
					PVariable[] mappedVariables = extractMappedVariables(typeConstraint);
					Tuple variablesTuple = Tuples.flatTupleOf((Object[]) mappedVariables);
					final IInputKey supplierKey = typeConstraint.getSupplierKey();
					if (SurrogateQueryRegistry.instance().hasSurrogateQueryFQN(supplierKey)) {
						PQuery surrogateQuery = SurrogateQueryRegistry.instance().getSurrogateQuery(supplierKey);
						if (surrogateQuery == null) {
							throw new IllegalStateException("Surrogate query for feature %s not found"
									.formatted(supplierKey.getPrettyPrintableName()));
						}
						addTrace(typeConstraint, new PositivePatternCall(getCopiedBody(), variablesTuple,
								surrogateQuery));
					} else {
						addTrace(typeConstraint, new TypeConstraint(getCopiedBody(), variablesTuple, supplierKey));
					}
				}
			};
			PBody modifiedBody = copier.getCopiedBody();
			replacedBodies.add(modifiedBody);
			modifiedBody.setStatus(PQuery.PQueryStatus.OK);
		}
		return new PDisjunction(replacedBodies);
	}
}
