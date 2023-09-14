/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.statecoding.stateequivalence;

import org.eclipse.collections.api.map.primitive.IntIntMap;
import tools.refinery.store.statecoding.Morphism;

import java.util.List;

public class PermutationMorphism implements Morphism {
	private final IntIntMap object2PermutationGroup;
	private final List<? extends List<? extends IntIntMap>> permutationsGroups;
	private final int[] selection;
	private boolean hasNext;

	PermutationMorphism(IntIntMap object2PermutationGroup,
						List<? extends List<? extends IntIntMap>> permutationsGroups) {
		this.object2PermutationGroup = object2PermutationGroup;
		this.permutationsGroups = permutationsGroups;

		this.selection = new int[this.permutationsGroups.size()];
		this.hasNext = true;
	}

	public boolean next() {
		return next(0);
	}

	private boolean next(int position) {
		if (position >= permutationsGroups.size()) {
			this.hasNext = false;
			return false;
		}
		if (selection[position] + 1 < permutationsGroups.get(position).size()) {
			selection[position] = selection[position] + 1;
			return true;
		} else {
			selection[position] = 0;
			return next(position + 1);
		}
	}

	@Override
	public int get(int object) {
		if(!hasNext) {
			throw new IllegalArgumentException("No next permutation!");
		}

		final int groupIndex = object2PermutationGroup.get(object);
		final var selectedGroup = permutationsGroups.get(groupIndex);
		final int permutationIndex = selection[groupIndex];
		final var selectedPermutation = selectedGroup.get(permutationIndex);

		return selectedPermutation.get(object);
	}

	@Override
	public int getSize() {
		return object2PermutationGroup.size();
	}
}
