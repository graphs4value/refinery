/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning.refinement;

public enum RefinementResult {
	UNCHANGED,
	REFINED,
	REJECTED;

	public RefinementResult andThen(RefinementResult next) {
		return switch (this) {
			case UNCHANGED -> next;
			case REFINED -> next == REJECTED ? REJECTED : REFINED;
			case REJECTED -> REJECTED;
		};
	}

	public boolean isRejected() {
		return this == REJECTED;
	}
}
