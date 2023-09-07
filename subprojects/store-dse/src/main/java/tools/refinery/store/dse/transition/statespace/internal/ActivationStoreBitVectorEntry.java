/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.dse.transition.statespace.internal;

public class ActivationStoreBitVectorEntry extends ActivationStoreEntry {
	final int[] selected;

	ActivationStoreBitVectorEntry(int numberOfActivations) {
		super(numberOfActivations);
		this.selected = new int[(numberOfActivations / Integer.SIZE) + 1];
	}

	@Override
	public int getNumberOfVisitedActivations() {
		int visited = 0;
		// Use indexed for loop to avoid allocating an iterator.
		//noinspection ForLoopReplaceableByForEach
		for (int i = 0; i < selected.length; i++) {
			visited += Integer.bitCount(selected[i]);
		}
		return visited;
	}

	private static final int ELEMENT_POSITION = 5; // size of Integer.SIZE
	private static final int ELEMENT_BITMASK = (1 << ELEMENT_POSITION) - 1;

	@Override
	public int getAndAddActivationAfter(int index) {
		int position = index;
		do {
			final int selectedElement = position >> ELEMENT_POSITION;
			final int selectedBit = 1 << (position & ELEMENT_BITMASK);

			if ((selected[selectedElement] & selectedBit) == 0) {
				selected[selectedElement] |= selectedBit;
				return position;
			} else {
				if (position < this.numberOfActivations - 1) {
					position++;
				} else {
					position = 0;
				}
			}
		} while (position != index);
		throw new IllegalArgumentException("There is are no unvisited activations!");
	}
}
