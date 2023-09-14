/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.dse.transition.statespace.internal;

import tools.refinery.store.dse.transition.VersionWithObjectiveValue;
import tools.refinery.store.dse.transition.statespace.EquivalenceClassStore;
import tools.refinery.store.statecoding.StateCoderResult;
import tools.refinery.store.statecoding.StateCoderStoreAdapter;

public abstract class AbstractEquivalenceClassStore implements EquivalenceClassStore {
	protected final StateCoderStoreAdapter stateCoderStoreAdapter;
	AbstractEquivalenceClassStore(StateCoderStoreAdapter stateCoderStoreAdapter) {
		this.stateCoderStoreAdapter = stateCoderStoreAdapter;
	}

	protected int numberOfUnresolvedSymmetries = 0;

	protected abstract void delegate(VersionWithObjectiveValue version, int[] emptyActivations, boolean accept);
	protected abstract boolean tryToAdd(StateCoderResult stateCoderResult, VersionWithObjectiveValue newVersion,
							   int[] emptyActivations, boolean accept);

	public abstract boolean tryToAdd(StateCoderResult stateCoderResult);

	@Override
	public boolean submit(StateCoderResult stateCoderResult) {
		return tryToAdd(stateCoderResult);
	}

	@Override
	public synchronized boolean submit(VersionWithObjectiveValue version, StateCoderResult stateCoderResult,
									   int[] emptyActivations, boolean accept) {
		boolean hasNewVersion = tryToAdd(stateCoderResult, version, emptyActivations, accept);
		if (hasNewVersion) {
			delegate(version, emptyActivations, accept);
			return true;
		} else {
			numberOfUnresolvedSymmetries++;
			return false;
		}
	}

	@Override
	public boolean hasUnresolvedSymmetry() {
		return numberOfUnresolvedSymmetries > 0;
	}

	@Override
	public int getNumberOfUnresolvedSymmetries() {
		return numberOfUnresolvedSymmetries;
	}
}
