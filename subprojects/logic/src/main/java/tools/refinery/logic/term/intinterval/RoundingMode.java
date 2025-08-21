/*
 * SPDX-FileCopyrightText: 2025 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.logic.term.intinterval;

public enum RoundingMode {
	CEIL {
		@Override
		public IntBound positiveInfinity() {
			return IntBound.Infinite.POSITIVE_INFINITY;
		}

		@Override
        public IntBound negativeInfinity() {
            return IntBound.Finite.MIN_VALUE;
        }

		@Override
		public IntBound error() {
			return IntBound.Infinite.NEGATIVE_INFINITY;
		}
	},
	FLOOR {
		@Override
		public IntBound positiveInfinity() {
			return IntBound.Finite.MAX_VALUE;
		}

		@Override
		public IntBound negativeInfinity() {
			return IntBound.Infinite.NEGATIVE_INFINITY;
		}

		@Override
		public IntBound error() {
			return IntBound.Infinite.POSITIVE_INFINITY;
		}
	};

	public abstract IntBound positiveInfinity();

	public abstract IntBound negativeInfinity();

	public abstract IntBound error();
}
