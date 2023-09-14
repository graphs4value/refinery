/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.dse.transition.objectives;

import tools.refinery.store.model.Model;
import tools.refinery.store.model.ModelStore;
import tools.refinery.store.model.ModelStoreBuilder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class CompositeObjective implements Objective {
	private final List<Objective> objectives;

	CompositeObjective(Collection<? extends Objective> objectives) {
		var unwrappedObjectives = new ArrayList<Objective>();
		for (var objective : objectives) {
			if (objective instanceof CompositeObjective compositeObjective) {
				unwrappedObjectives.addAll(compositeObjective.getObjectives());
			} else {
				unwrappedObjectives.add(objective);
			}
		}
		this.objectives = Collections.unmodifiableList(unwrappedObjectives);
	}

	public List<Objective> getObjectives() {
		return objectives;
	}

	@Override
	public boolean isAlwaysZero(ModelStore store) {
		for (var objective : objectives) {
			if (!objective.isAlwaysZero(store)) {
				return false;
			}
		}
		return true;
	}

	@Override
	public ObjectiveCalculator createCalculator(Model model) {
		var calculators = new ArrayList<ObjectiveCalculator>();
		for (var objective : objectives) {
			if (!objective.isAlwaysZero(model.getStore())) {
				calculators.add(objective.createCalculator(model));
			}
		}
		return () -> {
			double value = 0;
			for (var calculator : calculators) {
				value += calculator.getValue();
			}
			return value;
		};
	}

	@Override
	public void configure(ModelStoreBuilder storeBuilder) {
		for (var objective : objectives) {
			objective.configure(storeBuilder);
		}
	}
}
