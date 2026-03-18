/*
 * SPDX-FileCopyrightText: 2026 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning.smt;

import tools.refinery.store.model.ModelStoreBuilder;
import tools.refinery.store.reasoning.smt.expr.SmtExprChecker;
import tools.refinery.store.reasoning.theory.Theory;
import tools.refinery.store.reasoning.theory.TheoryRule;
import tools.refinery.store.reasoning.theory.TheorySupport;

import java.util.Collection;

public class Z3Theory implements Theory {
	private final SmtExprChecker checker = new SmtExprChecker();
	private final int timeout;
	private final int rlimit;

	public Z3Theory(int timeout, int rlimit) {
		this.timeout = timeout;
		this.rlimit = rlimit;
	}

	@Override
	public TheorySupport checkSupport(TheoryRule theoryRule) {
		if (checker.isSupported(theoryRule.assertedTerm())) {
			return TheorySupport.ENABLED_BY_DEFAULT;
		}
		return TheorySupport.UNSUPPORTED;
	}

	@Override
	public void createPropagator(ModelStoreBuilder storeBuilder, Collection<TheoryRule> collectedRules) {
		storeBuilder.with(new SmtPropagator()
				.rules(collectedRules)
				.timeout(timeout)
				.rlimit(rlimit));
	}
}
