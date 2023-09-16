/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.interpreter.matchers.psystem.basicenumerables;

import tools.refinery.interpreter.matchers.context.IQueryMetaContext;
import tools.refinery.interpreter.matchers.psystem.*;
import tools.refinery.interpreter.matchers.psystem.queries.PQuery;
import tools.refinery.interpreter.matchers.tuple.Tuple;

import java.util.Set;

public class RepresentativeElectionConstraint extends KeyedEnumerablePConstraint<PQuery>
		implements IQueryReference, ITypeInfoProviderConstraint {
	private final Connectivity connectivity;

	public RepresentativeElectionConstraint(PBody pBody, Tuple variablesTuple, PQuery supplierKey,
                                            Connectivity connectivity) {
		super(pBody, variablesTuple, supplierKey);
		this.connectivity = connectivity;
	}

	public Connectivity getConnectivity() {
		return connectivity;
	}

	@Override
	public PQuery getReferredQuery() {
		return supplierKey;
	}

	@Override
	public Set<TypeJudgement> getImpliedJudgements(IQueryMetaContext context) {
		return PositivePatternCall.getTypesImpliedByCall(supplierKey, variablesTuple);
	}

	@Override
	protected String keyToString() {
		return supplierKey.getFullyQualifiedName() + "#" + connectivity + "#representative";
	}
}
