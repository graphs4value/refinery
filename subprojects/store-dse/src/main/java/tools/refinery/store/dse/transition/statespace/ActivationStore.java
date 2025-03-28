/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.dse.transition.statespace;

import tools.refinery.store.dse.transition.VersionWithObjectiveValue;

import java.util.Random;

public interface ActivationStore {
	record VisitResult(boolean successfulVisit, boolean mayHaveMore, int transformation, int activation) {
	}

	// The return value of this method is only useful for exploration strategies that want to synchronise multiple
	// workers and avoid situtation when another worker has already visited the same version.
	@SuppressWarnings("UnusedReturnValue")
	VisitResult markNewAsVisited(VersionWithObjectiveValue to, int[] emptyEntrySizes);

	boolean hasUnmarkedActivation(VersionWithObjectiveValue version);

	VisitResult getRandomAndMarkAsVisited(VersionWithObjectiveValue version, Random random);
}
