/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.dse.transition;

import tools.refinery.store.adapter.ModelStoreAdapter;
import tools.refinery.store.dse.transition.objectives.Criterion;
import tools.refinery.store.dse.transition.objectives.Objective;
import tools.refinery.store.model.Model;

import java.util.List;

public interface DesignSpaceExplorationStoreAdapter extends ModelStoreAdapter
{
	@Override
	DesignSpaceExplorationAdapter createModelAdapter(Model model);

	List<Rule> getTransformations();

	List<Criterion> getAccepts();

	List<Criterion> getExcludes();

	List<Objective> getObjectives();
}
