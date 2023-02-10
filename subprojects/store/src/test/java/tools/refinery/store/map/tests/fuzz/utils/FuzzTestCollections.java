package tools.refinery.store.map.tests.fuzz.utils;

public final class FuzzTestCollections {
	public static final Object[] stepCounts = {FuzzTestUtils.FAST_STEP_COUNT};
	public static final Object[] keyCounts = {3, 32, 32 * 32};
	public static final Object[] valueCounts = {2, 3};
	public static final Object[] nullDefaultOptions = {false, true};
	public static final Object[] commitFrequencyOptions = {1, 10, 100};
	public static final Object[] randomSeedOptions = {1, 2, 3};
	public static final Object[] evilHashOptions = {false, true};
}
