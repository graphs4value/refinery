/*
 * SPDX-FileCopyrightText: 2025 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.logic.term.realinterval;

import java.math.MathContext;

public enum RoundingMode {
	CEIL {
		@Override
		public MathContext context() {
			return CEILING_CONTEXT;
		}

		@Override
		public RealBound error() {
			return RealBound.Infinite.NEGATIVE_INFINITY;
		}

		@Override
		public RealBound infinity() {
			return RealBound.Infinite.POSITIVE_INFINITY;
		}
	},
	FLOOR {
		@Override
        public MathContext context() {
            return FLOOR_CONTEXT;
        }

		@Override
        public RealBound error() {
            return RealBound.Infinite.POSITIVE_INFINITY;
        }

		@Override
        public RealBound infinity() {
            return RealBound.Infinite.NEGATIVE_INFINITY;
        }
	};

	private static final int PRECISION = 16;
	private static final MathContext CEILING_CONTEXT = new MathContext(PRECISION, java.math.RoundingMode.CEILING);
	private static final MathContext FLOOR_CONTEXT = new MathContext(PRECISION, java.math.RoundingMode.FLOOR);

	public abstract MathContext context();

	public abstract RealBound error();

	public abstract RealBound infinity();
}
