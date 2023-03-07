package tools.refinery.store.query.term.int_;

import tools.refinery.store.query.term.StatelessAggregator;

public final class IntSumAggregator implements StatelessAggregator<Integer, Integer> {
	public static final IntSumAggregator INSTANCE = new IntSumAggregator();

	private IntSumAggregator() {
	}

	@Override
	public Class<Integer> getResultType() {
		return Integer.class;
	}

	@Override
	public Class<Integer> getInputType() {
		return Integer.class;
	}

	@Override
	public Integer getEmptyResult() {
		return 0;
	}

	@Override
	public Integer add(Integer current, Integer value) {
		return current + value;
	}

	@Override
	public Integer remove(Integer current, Integer value) {
		return current - value;
	}
}
