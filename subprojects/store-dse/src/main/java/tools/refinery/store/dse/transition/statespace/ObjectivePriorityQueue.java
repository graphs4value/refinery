/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.dse.transition.statespace;

import tools.refinery.store.dse.transition.VersionWithObjectiveValue;
import tools.refinery.store.map.Version;

import java.util.Comparator;
import java.util.Random;

public interface ObjectivePriorityQueue {
	Comparator<VersionWithObjectiveValue> getComparator();
	void submit(VersionWithObjectiveValue versionWithObjectiveValue);
	void remove(VersionWithObjectiveValue versionWithObjectiveValue);
	int getSize();
	VersionWithObjectiveValue getBest();
	VersionWithObjectiveValue getRandom(Random random);
}
