/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.dse.transition.statespace;

import tools.refinery.store.dse.transition.VersionWithObjectiveValue;
import tools.refinery.store.statecoding.StateCoderResult;

public interface EquivalenceClassStore {
	boolean submit(VersionWithObjectiveValue version, StateCoderResult stateCoderResult, int[] emptyActivations, boolean accept);
	boolean submit(StateCoderResult stateCoderResult);
	boolean hasUnresolvedSymmetry();
	void resolveOneSymmetry();
	int getNumberOfUnresolvedSymmetries();
}
