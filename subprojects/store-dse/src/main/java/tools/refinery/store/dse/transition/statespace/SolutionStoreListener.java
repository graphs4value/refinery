/*
 * SPDX-FileCopyrightText: 2026 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.dse.transition.statespace;

import tools.refinery.store.dse.transition.VersionWithObjectiveValue;

public interface SolutionStoreListener {
	default void solutionAdded(VersionWithObjectiveValue version) {
	}

	default void solutionRemoved(VersionWithObjectiveValue version) {
	}
}
