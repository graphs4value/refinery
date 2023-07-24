/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning;

public enum MergeResult {
	UNCHANGED,
	REFINED,
	REJECTED;

	public MergeResult andAlso(MergeResult other) {
		return switch (this) {
			case UNCHANGED -> other;
			case REFINED -> other == REJECTED ? REJECTED : REFINED;
			case REJECTED -> REJECTED;
		};
	}
}
