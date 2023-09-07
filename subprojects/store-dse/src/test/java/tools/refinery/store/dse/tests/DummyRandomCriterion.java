/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.dse.tests;

import tools.refinery.store.dse.transition.objectives.Criterion;
import tools.refinery.store.dse.transition.objectives.CriterionCalculator;
import tools.refinery.store.model.Model;

import java.util.Random;

public class DummyRandomCriterion implements Criterion {

	@SuppressWarnings("squid:S2245")
	private static final Random random = new Random(9856654);
	public DummyRandomCriterion() {
	}

	@Override
	public CriterionCalculator createCalculator(Model model) {
		return random::nextBoolean;
	}
}
