/*
 * SPDX-FileCopyrightText: 2026 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning.theory;

import tools.refinery.store.model.ModelStoreBuilder;

import java.util.Collection;

public interface Theory {
	/**
	 * The default priority for theory propagators.
	 */
	int DEFAULT_PRIORITY = 0;

	/**
	 * The recommended priority for theory propagators that might diverge (i.e., execute continuously without
	 * reaching fixed point) on unsatisfiable problems.
	 * <p>
	 *     Propagators with this priority will run late enough in the sequence of propagators so that theory
	 *     propagators that can detect global unsatisfiability have already finished and determined that the
	 *     numerical problem is satisfiable.
	 * </p>
	 */
	int DIVERGING_PRIORITY = -100;

	TheorySupport checkSupport(TheoryRule theoryRule);

	void createPropagator(ModelStoreBuilder storeBuilder, Collection<TheoryRule> collectedRules);

	/**
	 * Gets the priority that determines where in the sequence of propagator should this theory run.
	 * <p>
	 *     Higher values denote a request to appear <i>earlier</i> in the sequence of propagators.
	 * </p>
	 *
	 * @return The priority. The default priority is {@link #DEFAULT_PRIORITY}.
	 */
	default int getPriority() {
		return DEFAULT_PRIORITY;
	}
}
