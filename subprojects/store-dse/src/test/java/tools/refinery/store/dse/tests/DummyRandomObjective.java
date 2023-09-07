/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.dse.tests;

import tools.refinery.store.dse.transition.objectives.Objective;
import tools.refinery.store.dse.transition.objectives.ObjectiveCalculator;
import tools.refinery.store.model.Model;

import java.util.Random;

public class DummyRandomObjective implements Objective {

	@SuppressWarnings("squid:S2245")
	private static final Random random = new Random(9856654);

	@Override
	public ObjectiveCalculator createCalculator(Model model) {
		return random::nextDouble;
	}
}
