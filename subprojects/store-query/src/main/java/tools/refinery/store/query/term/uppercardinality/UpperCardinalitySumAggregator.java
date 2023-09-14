/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.query.term.uppercardinality;

import tools.refinery.store.query.term.StatefulAggregate;
import tools.refinery.store.query.term.StatefulAggregator;
import tools.refinery.store.representation.cardinality.FiniteUpperCardinality;
import tools.refinery.store.representation.cardinality.UnboundedUpperCardinality;
import tools.refinery.store.representation.cardinality.UpperCardinalities;
import tools.refinery.store.representation.cardinality.UpperCardinality;

public class UpperCardinalitySumAggregator implements StatefulAggregator<UpperCardinality, UpperCardinality> {
	public static final UpperCardinalitySumAggregator INSTANCE = new UpperCardinalitySumAggregator();

	private UpperCardinalitySumAggregator() {
	}

	@Override
	public Class<UpperCardinality> getResultType() {
		return UpperCardinality.class;
	}

	@Override
	public Class<UpperCardinality> getInputType() {
		return UpperCardinality.class;
	}

	@Override
	public StatefulAggregate<UpperCardinality, UpperCardinality> createEmptyAggregate() {
		return new Aggregate();
	}

	private static class Aggregate implements StatefulAggregate<UpperCardinality, UpperCardinality> {
		private int sumFiniteUpperBounds;
		private int countUnbounded;

		public Aggregate() {
			this(0, 0);
		}

		private Aggregate(int sumFiniteUpperBounds, int countUnbounded) {
			this.sumFiniteUpperBounds = sumFiniteUpperBounds;
			this.countUnbounded = countUnbounded;
		}

		@Override
		public void add(UpperCardinality value) {
			if (value instanceof FiniteUpperCardinality finiteUpperCardinality) {
				sumFiniteUpperBounds += finiteUpperCardinality.finiteUpperBound();
			} else if (value instanceof UnboundedUpperCardinality) {
				countUnbounded += 1;
			} else {
				throw new IllegalArgumentException("Unknown UpperCardinality: " + value);
			}
		}

		@Override
		public void remove(UpperCardinality value) {
			if (value instanceof FiniteUpperCardinality finiteUpperCardinality) {
				sumFiniteUpperBounds -= finiteUpperCardinality.finiteUpperBound();
			} else if (value instanceof UnboundedUpperCardinality) {
				countUnbounded -= 1;
			} else {
				throw new IllegalArgumentException("Unknown UpperCardinality: " + value);
			}
		}

		@Override
		public UpperCardinality getResult() {
			return countUnbounded > 0 ? UpperCardinalities.UNBOUNDED : UpperCardinalities.atMost(sumFiniteUpperBounds);
		}

		@Override
		public boolean isEmpty() {
			return sumFiniteUpperBounds == 0 && countUnbounded == 0;
		}

		@Override
		public StatefulAggregate<UpperCardinality, UpperCardinality> deepCopy() {
			return new Aggregate(sumFiniteUpperBounds, countUnbounded);
		}
	}
}
