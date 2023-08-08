/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.dse;

import tools.refinery.store.adapter.ModelStoreAdapter;
import tools.refinery.store.dse.internal.TransformationRule;
import tools.refinery.store.dse.objectives.Objective;
import tools.refinery.store.model.Model;
import tools.refinery.store.query.dnf.RelationalQuery;

import java.util.List;
import java.util.Set;

public interface DesignSpaceExplorationStoreAdapter extends ModelStoreAdapter {

	@Override
	DesignSpaceExplorationAdapter createModelAdapter(Model model);

	Set<TransformationRule> getTransformationSpecifications();

	Set<RelationalQuery> getGlobalConstraints();

	List<Objective> getObjectives();

	Strategy getStrategy();
}
