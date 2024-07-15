/*
 * SPDX-FileCopyrightText: 2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.dse.propagation;

public enum PropagationSuccessResult implements PropagationResult {
	UNCHANGED,
	PROPAGATED;

	@Override
	public PropagationResult andThen(PropagationResult next) {
		return switch (this) {
			case UNCHANGED -> next;
			case PROPAGATED -> next instanceof PropagationRejectedResult ? next : PROPAGATED;
		};
	}

	@Override
	public boolean isRejected() {
		return false;
	}

	@Override
	public void throwIfRejected() {
		// Nothing to throw.
	}

	@Override
	public boolean isChanged() {
		return this == PROPAGATED;
	}
}
