/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.dse.transition;

import tools.refinery.store.adapter.ModelAdapter;
import tools.refinery.store.dse.transition.internal.DesignSpaceExplorationBuilderImpl;

import java.util.List;

public interface DesignSpaceExplorationAdapter extends ModelAdapter {
	@Override
	DesignSpaceExplorationStoreAdapter getStoreAdapter();

	static DesignSpaceExplorationBuilder builder() {
		return new DesignSpaceExplorationBuilderImpl();
	}

	List<Transformation> getTransformations();

	boolean checkAccept();

	boolean checkExclude();

	ObjectiveValue getObjectiveValue();
}
