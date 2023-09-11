/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.dse.transition.internal;

import tools.refinery.store.dse.transition.DesignSpaceExplorationAdapter;
import tools.refinery.store.dse.transition.DesignSpaceExplorationStoreAdapter;
import tools.refinery.store.dse.transition.ObjectiveValue;
import tools.refinery.store.dse.transition.Transformation;
import tools.refinery.store.dse.transition.objectives.CriterionCalculator;
import tools.refinery.store.dse.transition.objectives.ObjectiveCalculator;
import tools.refinery.store.model.Model;

import java.util.List;

public class DesignSpaceExplorationAdapterImpl implements DesignSpaceExplorationAdapter {
	final Model model;
	final DesignSpaceExplorationStoreAdapter designSpaceExplorationStoreAdapter;

	final List<Transformation> transformations;
	final List<CriterionCalculator> accepts;
	final List<CriterionCalculator> excludes;
	final List<ObjectiveCalculator> objectives;

	public DesignSpaceExplorationAdapterImpl(Model model,
											 DesignSpaceExplorationStoreAdapter designSpaceExplorationStoreAdapter,
											 List<Transformation> transformations,
											 List<CriterionCalculator> accepts,
											 List<CriterionCalculator> excludes,
											 List<ObjectiveCalculator> objectives) {
		this.model = model;
		this.designSpaceExplorationStoreAdapter = designSpaceExplorationStoreAdapter;

		this.transformations = transformations;
		this.accepts = accepts;
		this.excludes = excludes;
		this.objectives = objectives;
	}

	@Override
	public Model getModel() {
		return model;
	}

	@Override
	public DesignSpaceExplorationStoreAdapter getStoreAdapter() {
		return designSpaceExplorationStoreAdapter;
	}

	public List<Transformation> getTransformations() {
		return transformations;
	}

	@Override
	public boolean checkAccept() {
		for (var accept : this.accepts) {
			model.checkCancelled();
			if (!accept.isSatisfied()) {
				return false;
			}
		}
		return true;
	}

	@Override
	public boolean checkExclude() {
		for (var exclude : this.excludes) {
			model.checkCancelled();
			if (exclude.isSatisfied()) {
				return true;
			}
		}
		return false;
	}

	@Override
	public ObjectiveValue getObjectiveValue() {
		model.checkCancelled();
		if (objectives.size() == 1) {
			return ObjectiveValue.of(objectives.get(0).getValue());
		} else if (objectives.size() == 2) {
			return ObjectiveValue.of(objectives.get(0).getValue(), objectives.get(1).getValue());
		} else {
			double[] res = new double[objectives.size()];
			for (int i = 0; i < objectives.size(); i++) {
				model.checkCancelled();
				res[i] = objectives.get(i).getValue();
			}
			return ObjectiveValue.of(res);
		}
	}
}
