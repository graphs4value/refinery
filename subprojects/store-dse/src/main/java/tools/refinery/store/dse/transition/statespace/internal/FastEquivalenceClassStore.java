/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.dse.transition.statespace.internal;

import org.eclipse.collections.api.factory.primitive.IntSets;
import org.eclipse.collections.api.set.primitive.MutableIntSet;
import tools.refinery.store.dse.transition.VersionWithObjectiveValue;
import tools.refinery.store.dse.transition.statespace.EquivalenceClassStore;
import tools.refinery.store.statecoding.StateCoderResult;
import tools.refinery.store.statecoding.StateCoderStoreAdapter;

public abstract class FastEquivalenceClassStore extends AbstractEquivalenceClassStore implements EquivalenceClassStore {

	private final MutableIntSet codes = IntSets.mutable.empty();

	protected FastEquivalenceClassStore(StateCoderStoreAdapter stateCoderStoreAdapter) {
		super(stateCoderStoreAdapter);
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
