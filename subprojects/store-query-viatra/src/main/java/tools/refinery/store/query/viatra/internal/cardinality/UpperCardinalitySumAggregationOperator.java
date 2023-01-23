package tools.refinery.store.query.viatra.internal.cardinality;

import org.eclipse.viatra.query.runtime.matchers.psystem.aggregations.BoundAggregator;
import org.eclipse.viatra.query.runtime.matchers.psystem.aggregations.IMultisetAggregationOperator;
import tools.refinery.store.representation.cardinality.FiniteUpperCardinality;
import tools.refinery.store.representation.cardinality.UnboundedUpperCardinality;
import tools.refinery.store.representation.cardinality.UpperCardinalities;
import tools.refinery.store.representation.cardinality.UpperCardinality;

import java.util.stream.Stream;

public class UpperCardinalitySumAggregationOperator implements IMultisetAggregationOperator<UpperCardinality,
		UpperCardinalitySumAggregationOperator.Accumulator, UpperCardinality> {
	public static final UpperCardinalitySumAggregationOperator INSTANCE = new UpperCardinalitySumAggregationOperator();

	public static final BoundAggregator BOUND_AGGREGATOR = new BoundAggregator(INSTANCE, UpperCardinality.class,
			UpperCardinality.class);

	private UpperCardinalitySumAggregationOperator() {
		// Singleton constructor.
	}

	@Override
	public String getName() {
		return "sum<UpperCardinality>";
	}

	@Override
	public String getShortDescription() {
		return "%s computes the sum of finite or unbounded upper cardinalities".formatted(getName());
	}

	@Override
	public Accumulator createNeutral() {
		return new Accumulator();
	}

	@Override
	public boolean isNeutral(Accumulator result) {
		return result.sumFiniteUpperBounds == 0 && result.countUnbounded == 0;
	}

	@Override
	public Accumulator update(Accumulator oldResult, UpperCardinality updateValue, boolean isInsertion) {
		if (updateValue instanceof FiniteUpperCardinality finiteUpperCardinality) {
			int finiteUpperBound = finiteUpperCardinality.finiteUpperBound();
			if (isInsertion) {
				oldResult.sumFiniteUpperBounds += finiteUpperBound;
			} else {
				oldResult.sumFiniteUpperBounds -= finiteUpperBound;
			}
		} else if (updateValue instanceof UnboundedUpperCardinality) {
			if (isInsertion) {
				oldResult.countUnbounded += 1;
			} else {
				oldResult.countUnbounded -= 1;
			}
		} else {
			throw new IllegalArgumentException("Unknown UpperCardinality: " + updateValue);
		}
		return oldResult;
	}

	@Override
	public UpperCardinality getAggregate(Accumulator result) {
		return result.countUnbounded > 0 ? UpperCardinalities.UNBOUNDED :
				UpperCardinalities.valueOf(result.sumFiniteUpperBounds);
	}

	@Override
	public UpperCardinality aggregateStream(Stream<UpperCardinality> stream) {
		var result = stream.collect(this::createNeutral, (accumulator, value) -> update(accumulator, value, true),
				(left, right) -> new Accumulator(left.sumFiniteUpperBounds + right.sumFiniteUpperBounds,
						left.countUnbounded + right.countUnbounded));
		return getAggregate(result);
	}

	@Override
	public Accumulator clone(Accumulator original) {
		return new Accumulator(original.sumFiniteUpperBounds, original.countUnbounded);
	}

	public static class Accumulator {
		private int sumFiniteUpperBounds;

		private int countUnbounded;

		private Accumulator(int sumFiniteUpperBounds, int countUnbounded) {
			this.sumFiniteUpperBounds = sumFiniteUpperBounds;
			this.countUnbounded = countUnbounded;
		}

		private Accumulator() {
			this(0, 0);
		}
	}
}
