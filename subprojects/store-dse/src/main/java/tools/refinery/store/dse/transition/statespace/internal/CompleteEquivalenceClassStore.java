/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.dse.transition.statespace.internal;

import org.eclipse.collections.api.factory.primitive.IntObjectMaps;
import org.eclipse.collections.api.map.primitive.MutableIntObjectMap;
import tools.refinery.store.dse.transition.VersionWithObjectiveValue;
import tools.refinery.store.dse.transition.statespace.EquivalenceClassStore;
import tools.refinery.store.statecoding.StateCoderResult;
import tools.refinery.store.statecoding.StateCoderStoreAdapter;
import tools.refinery.store.statecoding.StateEquivalenceChecker;

import java.util.ArrayList;

public abstract class CompleteEquivalenceClassStore extends AbstractEquivalenceClassStore implements EquivalenceClassStore {

	static class SymmetryStoreArray extends ArrayList<VersionWithObjectiveValue> {
		final int[] activationSizes;
		final boolean accept;

		SymmetryStoreArray(int[] activationSizes, boolean accept) {
			super();
			this.activationSizes = activationSizes;
			this.accept = accept;
		}
	}

	private final MutableIntObjectMap<Object> modelCode2Versions = IntObjectMaps.mutable.empty();

	protected CompleteEquivalenceClassStore(StateCoderStoreAdapter stateCoderStoreAdapter) {
		super(stateCoderStoreAdapter);
	}

	@Override
	protected boolean tryToAdd(StateCoderResult stateCoderResult, VersionWithObjectiveValue newVersion,
					   int[] emptyActivations, boolean accept) {
		int modelCode = stateCoderResult.modelCode();
		Object old = modelCode2Versions.updateValue(
				modelCode,
				() -> newVersion,
				x -> {
					if (x instanceof SymmetryStoreArray array) {
						if(array.accept != accept || array.activationSizes != emptyActivations) {
							this.delegate(newVersion,emptyActivations,accept);
							return x;
						}
						array.add(newVersion);
						return array;
					} else {
						SymmetryStoreArray result = new SymmetryStoreArray(emptyActivations, accept);
						result.add((VersionWithObjectiveValue) x);
						result.add(newVersion);
						return result;
					}
				});
		return old == null;
	}

	@Override
	public void resolveOneSymmetry() {
		var unresolvedSimilarity = getOneUnresolvedSymmetry();
		if (unresolvedSimilarity == null) {
			return;
		}
		var outcome = this.stateCoderStoreAdapter.checkEquivalence(unresolvedSimilarity.get(0).version(),
				unresolvedSimilarity.get(1).version());
		if (outcome != StateEquivalenceChecker.EquivalenceResult.ISOMORPHIC) {
			delegate(unresolvedSimilarity.get(1), unresolvedSimilarity.activationSizes, unresolvedSimilarity.accept);
		}
	}

	//record  UnresolvedSymmetryResult

	private synchronized SymmetryStoreArray getOneUnresolvedSymmetry() {
		if (numberOfUnresolvedSymmetries <= 0) {
			return null;
		}

		for (var entry : modelCode2Versions.keyValuesView()) {
			int hash = entry.getOne();
			var value = entry.getTwo();
			if (value instanceof SymmetryStoreArray array) {
				int size = array.size();
				var representative = array.get(0);
				var similar = array.get(size - 1);
				array.remove(size - 1);

				if (size <= 2) {
					modelCode2Versions.put(hash, representative);
				}

				var result = new SymmetryStoreArray(array.activationSizes, array.accept);
				result.add(representative);
				result.add(similar);
				return result;
			}
		}

		return null;
	}
}
