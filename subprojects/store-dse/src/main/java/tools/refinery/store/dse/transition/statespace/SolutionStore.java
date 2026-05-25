/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.dse.transition.statespace;

import tools.refinery.store.dse.transition.VersionWithObjectiveValue;

import java.util.List;

public interface SolutionStore {
	boolean submit(VersionWithObjectiveValue version);

	List<VersionWithObjectiveValue> getSolutions();

	boolean hasEnoughSolution();

	void addListener(SolutionStoreListener listener);

	void removeListener(SolutionStoreListener listener);
}
