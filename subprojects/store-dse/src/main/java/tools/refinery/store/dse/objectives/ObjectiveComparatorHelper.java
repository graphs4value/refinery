/*******************************************************************************
 * Copyright (c) 2010-2015, Andras Szabolcs Nagy, Abel Hegedus, Akos Horvath, Zoltan Ujhelyi and Daniel Varro
 * Copyright (c) 2023 The Refinery Authors <https://refinery.tools/>
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.store.dse.objectives;

import java.util.List;

/**
 * This class is responsible to compare and sort fitness values.
 *
 * @author András Szabolcs Nagy
 */
public class ObjectiveComparatorHelper {

	private final List<Objective> objectives;

	public ObjectiveComparatorHelper(List<Objective> objectives) {
		this.objectives = objectives;
	}

	/**
	 * Compares two fitnesses based on dominance. Returns -1 if the second parameter {@code o2} is a better
	 * solution ({@code o2} dominates {@code o1}), 1 if the first parameter {@code o1} is better ({@code o1} dominates
	 * {@code o2}) and returns 0 if they are non-dominating each other.
	 */
	public int compare(Fitness o1, Fitness o2) {

		boolean o1HasBetterFitness = false;
		boolean o2HasBetterFitness = false;

		for (Objective objective : objectives) {
			String objectiveName = objective.getName();
			int sgn = objective.getComparator().compare(o1.get(objectiveName), o2.get(objectiveName));

			if (sgn < 0) {
				o2HasBetterFitness = true;
			}
			if (sgn > 0) {
				o1HasBetterFitness = true;
			}
			if (o1HasBetterFitness && o2HasBetterFitness) {
				break;
			}
		}
		if (o2HasBetterFitness) {
			return -1;
		} else if (o1HasBetterFitness) {
			return 1;
		}

		return 0;

	}
}
