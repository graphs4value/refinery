/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.dse.transition.objectives;

import tools.refinery.store.model.ModelStoreBuilder;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;

public abstract sealed class CompositeCriterion implements Criterion permits AndCriterion, OrCriterion {
	private final List<Criterion> criteria;

	protected CompositeCriterion(Collection<? extends Criterion> criteria) {
		var deDuplicatedCriteria = new LinkedHashSet<Criterion>();
		for (var criterion : criteria) {
			if (criterion.getClass() == this.getClass()) {
				var childCriteria = ((CompositeCriterion) criterion).getCriteria();
				deDuplicatedCriteria.addAll(childCriteria);
			} else {
				deDuplicatedCriteria.add(criterion);
			}
		}
		this.criteria = List.copyOf(deDuplicatedCriteria);
	}

	public List<Criterion> getCriteria() {
		return criteria;
	}

	@Override
	public void configure(ModelStoreBuilder storeBuilder) {
		for (var criterion : criteria) {
			criterion.configure(storeBuilder);
		}
	}
}
