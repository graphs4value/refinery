/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.dse.transition.objectives;

import tools.refinery.store.model.Model;
import tools.refinery.store.model.ModelStore;
import tools.refinery.store.query.literal.Reduction;

import java.util.ArrayList;
import java.util.Collection;

public final class AndCriterion extends CompositeCriterion {
	AndCriterion(Collection<? extends Criterion> criteria) {
		super(criteria);
	}

	@Override
	public Reduction getReduction(ModelStore store) {
		for (var criterion : getCriteria()) {
			var reduction = criterion.getReduction(store);
			if (reduction == Reduction.ALWAYS_FALSE) {
				return Reduction.ALWAYS_FALSE;
			} else if (reduction == Reduction.NOT_REDUCIBLE) {
				return Reduction.NOT_REDUCIBLE;
			}
		}
		return Reduction.ALWAYS_TRUE;
	}

	@Override
	public CriterionCalculator createCalculator(Model model) {
		var calculators = new ArrayList<CriterionCalculator>();
		for (var criterion : getCriteria()) {
			var reduction = criterion.getReduction(model.getStore());
			if (reduction == Reduction.ALWAYS_FALSE) {
				return () -> false;
			} else if (reduction == Reduction.NOT_REDUCIBLE) {
				calculators.add(criterion.createCalculator(model));
			}
		}
		return () -> {
			for (var calculator : calculators) {
				if (!calculator.isSatisfied()) {
					return false;
				}
			}
			return true;
		};
	}
}
