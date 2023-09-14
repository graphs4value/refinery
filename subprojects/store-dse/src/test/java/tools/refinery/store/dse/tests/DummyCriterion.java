/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.dse.tests;

import tools.refinery.store.dse.transition.objectives.Criterion;
import tools.refinery.store.dse.transition.objectives.CriterionCalculator;
import tools.refinery.store.model.Model;

public class DummyCriterion implements Criterion {
	protected final boolean returnValue;
	public DummyCriterion(boolean returnValue) {
		this.returnValue = returnValue;
	}

	@Override
	public CriterionCalculator createCalculator(Model model) {
		return () -> returnValue;
	}
}
