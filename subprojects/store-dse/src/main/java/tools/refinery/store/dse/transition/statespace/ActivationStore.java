/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.dse.transition.statespace;

import tools.refinery.store.dse.transition.VersionWithObjectiveValue;
import tools.refinery.store.map.Version;

import java.util.Random;

public interface ActivationStore {
	record VisitResult(boolean successfulVisit, boolean mayHaveMore, int transformation, int activation) { }
	VisitResult markNewAsVisited(VersionWithObjectiveValue to, int[] emptyEntrySizes);
	boolean hasUnmarkedActivation(VersionWithObjectiveValue version);
	VisitResult getRandomAndMarkAsVisited(VersionWithObjectiveValue version, Random random);
}
