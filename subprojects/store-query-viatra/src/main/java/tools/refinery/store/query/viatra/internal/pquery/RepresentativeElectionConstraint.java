/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.query.viatra.internal.pquery;

import org.eclipse.viatra.query.runtime.matchers.context.IQueryMetaContext;
import org.eclipse.viatra.query.runtime.matchers.psystem.*;
import org.eclipse.viatra.query.runtime.matchers.psystem.basicenumerables.PositivePatternCall;
import org.eclipse.viatra.query.runtime.matchers.psystem.queries.PQuery;
import org.eclipse.viatra.query.runtime.matchers.tuple.Tuple;
import tools.refinery.store.query.literal.Connectivity;

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
		return supplierKey.getFullyQualifiedName() + "#representative";
	}
}
