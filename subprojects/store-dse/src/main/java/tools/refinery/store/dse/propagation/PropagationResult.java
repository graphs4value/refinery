/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.dse.propagation;

public enum PropagationResult {
	UNCHANGED,
	PROPAGATED,
	REJECTED;

	public PropagationResult andThen(PropagationResult next) {
		return switch (this) {
			case UNCHANGED -> next;
			case PROPAGATED -> next == REJECTED ? REJECTED : PROPAGATED;
			case REJECTED -> REJECTED;
		};
	}

	public boolean isRejected() {
		return this == REJECTED;
	}

	public boolean isChanged() {
		return this == PROPAGATED;
	}
}
