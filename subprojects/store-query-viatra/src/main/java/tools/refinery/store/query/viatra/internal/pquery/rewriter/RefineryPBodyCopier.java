/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.query.viatra.internal.pquery.rewriter;

import org.eclipse.viatra.query.runtime.matchers.psystem.PBody;
import org.eclipse.viatra.query.runtime.matchers.psystem.PConstraint;
import org.eclipse.viatra.query.runtime.matchers.psystem.rewriters.IRewriterTraceCollector;
import org.eclipse.viatra.query.runtime.matchers.psystem.rewriters.PBodyCopier;
import org.eclipse.viatra.query.runtime.matchers.tuple.Tuples;
import tools.refinery.store.query.viatra.internal.pquery.RepresentativeElectionConstraint;

public class RefineryPBodyCopier extends PBodyCopier {
	public RefineryPBodyCopier(PBody body, IRewriterTraceCollector traceCollector) {
		super(body, traceCollector);
	}

	@Override
	protected void copyConstraint(PConstraint constraint) {
		if (constraint instanceof RepresentativeElectionConstraint representativeElectionConstraint) {
			copyRepresentativeElectionConstraint(representativeElectionConstraint);
		} else {
			super.copyConstraint(constraint);
		}
	}

	private void copyRepresentativeElectionConstraint(RepresentativeElectionConstraint constraint) {
		var mappedVariables = extractMappedVariables(constraint);
		var variablesTuple = Tuples.flatTupleOf((Object[]) mappedVariables);
		addTrace(constraint, new RepresentativeElectionConstraint(body, variablesTuple, constraint.getReferredQuery(),
				constraint.getConnectivity()));
	}
}
