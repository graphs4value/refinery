/*
 * SPDX-FileCopyrightText: 2025 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.logic.term.intinterval;

public enum RoundingMode {
	CEIL {
		@Override
		public IntBound error() {
			return IntBound.Infinite.NEGATIVE_INFINITY;
		}
	},
	FLOOR {
		@Override
		public IntBound error() {
			return IntBound.Infinite.POSITIVE_INFINITY;
		}
	};

	public abstract IntBound error();
}
