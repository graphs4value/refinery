/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.dse.transition.statespace.internal;

import org.eclipse.collections.api.factory.primitive.IntLists;
import org.eclipse.collections.api.list.primitive.MutableIntList;

public class ActivationStoreListEntry extends ActivationStoreEntry {
	private final MutableIntList visitedActivations = IntLists.mutable.empty();

	ActivationStoreListEntry(int numberOfActivations) {
		super(numberOfActivations);
	}

	@Override
	public int getNumberOfUnvisitedActivations() {
		return this.numberOfActivations - visitedActivations.size();
	}

	@Override
	public int getAndAddActivationAfter(int index) {
		// If it is empty, just add it.
		if(this.visitedActivations.isEmpty()) {
			this.visitedActivations.add(index);
			return index;
		}

		int position = getPosition(index);

		// If the index is not in the position, one can insert it
		if(this.visitedActivations.get(position) != index) {
			this.visitedActivations.addAtIndex(position,index);
			return index;
		}

		// Otherwise, get the next empty space between two elements
		while(position + 2 < this.visitedActivations.size()) {
			position++;
			if(this.visitedActivations.get(position+1)-this.visitedActivations.get(position) > 1) {
				this.visitedActivations.addAtIndex(position+1, this.visitedActivations.get(position+1)+1);
			}
		}

		// Otherwise, try to add to the last space
		int last = this.visitedActivations.get(this.visitedActivations.size()-1);
		if(last<this.numberOfActivations) {
			this.visitedActivations.add(last+1);
			return last+1;
		}

		// Otherwise, try to put to the beginning
		if(this.visitedActivations.get(0) > 0) {
			this.visitedActivations.add(0);
			return 0;
		}

		// Otherwise, get the next empty space between two elements
		position = 0;
		while(position + 2 < this.visitedActivations.size()) {
			position++;
			if(this.visitedActivations.get(position+1)-this.visitedActivations.get(position) > 1) {
				this.visitedActivations.addAtIndex(position+1, this.visitedActivations.get(position+1)+1);
			}
		}

		throw new IllegalArgumentException("There is are no unvisited activations!");
	}

	/**
	 * Returns the position of an index in the {@code visitedActivations}. If the collection contains the index, in
	 * returns its position, otherwise, it  returns the position where the index need to be put.
	 *
	 * @param index Index of an activation.
	 * @return The position of the index.
	 */
	private int getPosition(int index) {
		int left = 0;
		int right = this.visitedActivations.size() - 1;
		while (left <= right) {
			final int middle = (right - left) / 2 + left;
			final int middleElement = visitedActivations.get(middle);
			if(middleElement == index) {
				return middle;
			} else if(middleElement < index) {
				left = middle +1;
			} else{
				right = middle-1;
			}
		}
		return right+1;
	}
}
