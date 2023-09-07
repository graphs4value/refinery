/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.dse.transition.statespace.internal;

import org.eclipse.collections.impl.set.mutable.primitive.IntHashSet;
import tools.refinery.store.dse.transition.VersionWithObjectiveValue;
import tools.refinery.store.dse.transition.statespace.EquivalenceClassStore;
import tools.refinery.store.statecoding.StateCoderResult;
import tools.refinery.store.statecoding.StateCoderStoreAdapter;

public abstract class FastEquivalenceClassStore extends AbstractEquivalenceClassStore implements EquivalenceClassStore {

	final IntHashSet codes;

	protected FastEquivalenceClassStore(StateCoderStoreAdapter stateCoderStoreAdapter) {
		super(stateCoderStoreAdapter);
		this.codes = new IntHashSet();
	}

	@Override
	protected synchronized boolean tryToAdd(StateCoderResult stateCoderResult, VersionWithObjectiveValue newVersion,
								int[] emptyActivations, boolean accept) {
		return this.codes.add(stateCoderResult.modelCode());
	}

	public synchronized boolean tryToAdd(StateCoderResult stateCoderResult) {
		return this.codes.add(stateCoderResult.modelCode());
	}

	@Override
	public void resolveOneSymmetry() {
		throw new IllegalArgumentException("This equivalence storage is not prepared to resolve symmetries!");
	}
}
