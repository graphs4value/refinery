/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.dse.transition.statespace.internal;

public abstract class ActivationStoreEntry {
	protected final int numberOfActivations;

	ActivationStoreEntry(int numberOfActivations) {
		this.numberOfActivations = numberOfActivations;
	}

	public abstract int getNumberOfVisitedActivations();

	public int getNumberOfUnvisitedActivations() {
		return numberOfActivations - getNumberOfVisitedActivations();
	}

	public int getNumberOfActivations() {
		return numberOfActivations;
	}

	public abstract int getAndAddActivationAfter(int index);

	//	public abstract boolean contains(int activation)
	//	public abstract boolean add(int activation)

	public static ActivationStoreEntry create(int size) {
		if(size <= Integer.SIZE*6) {
			return new ActivationStoreBitVectorEntry(size);
		} else {
			return new ActivationStoreListEntry(size);
		}
	}
}
