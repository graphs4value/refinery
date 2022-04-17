package tools.refinery.store.map.benchmarks.getdiffcursor;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Fork(1)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Measurement(time = 1, timeUnit = TimeUnit.SECONDS)
@Warmup(time = 1, timeUnit = TimeUnit.SECONDS)
public class ImmutableGetDiffCursorBenchmark {

	@Benchmark
	public void immutableGetDiffCursorBenchmark(ImmutableGetDiffCursorExecutionPlan executionPlan, Blackhole blackhole) {
		var sutFilledAndCommitted = executionPlan.getSut();
		int nCommit = executionPlan.getnCommit();
		for (int i = 0; i < executionPlan.nGetDiffCursor; i++) {
			sutFilledAndCommitted.getDiffCursor(i % nCommit);
		}
		blackhole.consume(sutFilledAndCommitted);
	}

	@Benchmark
	public void immutableGetDiffCursorAndCommitBenchmark(ImmutableGetDiffCursorExecutionPlan executionPlan, Blackhole blackhole) {
		var sutFilledAndCommitted = executionPlan.getSut();
		int nCommit = executionPlan.getnCommit();
		for (int i = 0; i < executionPlan.nGetDiffCursor; i++) {
			sutFilledAndCommitted.getDiffCursor(i % nCommit);
			if (i % 10 == 0) {
				blackhole.consume(sutFilledAndCommitted.commit());
			}
		}
		blackhole.consume(sutFilledAndCommitted);
	}

	@Benchmark
	public void baselineGetDiffCursorBenchmark(ImmutableGetDiffCursorExecutionPlan executionPlan, Blackhole blackhole) {
		var sutFilled = executionPlan.getFilledHashMap();
		var store = executionPlan.getStore();
		int nCommit = executionPlan.getnCommit();
		for (int i = 0; i < executionPlan.nGetDiffCursor; i++) {
			Map<Integer, String> mapFromStore = store.get(i % nCommit);
			Map<Integer, String> difference = new HashMap<>();
			difference.putAll(sutFilled);
			difference.putAll(mapFromStore);
			difference.entrySet().removeAll(mapFromStore.entrySet());
		}

		blackhole.consume(sutFilled);
		blackhole.consume(store);
	}

	@Benchmark
	public void baselineGetDiffCursorAndCommitBenchmark(ImmutableGetDiffCursorExecutionPlan executionPlan, Blackhole blackhole) {
		var sutFilled = executionPlan.getFilledHashMap();
		var store = executionPlan.getStore();
		int nCommit = executionPlan.getnCommit();
		for (int i = 0; i < executionPlan.nGetDiffCursor; i++) {
			Map<Integer, String> mapFromStore = store.get(i % nCommit);
			Map<Integer, String> difference = new HashMap<>();
			difference.putAll(sutFilled);
			difference.putAll(mapFromStore);
			difference.entrySet().removeAll(mapFromStore.entrySet());
			if (i % 10 == 0) {
				blackhole.consume(store.add(mapFromStore));
			}
		}

		blackhole.consume(sutFilled);
		blackhole.consume(store);
	}
}
