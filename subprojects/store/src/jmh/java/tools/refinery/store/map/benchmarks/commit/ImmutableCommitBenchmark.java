package tools.refinery.store.map.benchmarks.commit;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

@Fork(1)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Measurement(time = 1, timeUnit = TimeUnit.SECONDS)
@Warmup(time = 1, timeUnit = TimeUnit.SECONDS)
public class ImmutableCommitBenchmark {

	@Benchmark
	public void immutableCommitBenchmark(ImmutableCommitExecutionPlan executionPlan, Blackhole blackhole) {
		var sut = executionPlan.getSut();
		for (int i = 0; i < executionPlan.nCommit; i++) {
			blackhole.consume(sut.commit());
		}
		blackhole.consume(sut);
	}

	@Benchmark
	public void baselineCommitBenchmark(ImmutableCommitExecutionPlan executionPlan, Blackhole blackhole) {
		var sut = executionPlan.getFilledHashMap();
		var store = new ArrayList<HashMap<Integer, String>>();
		for (int i = 0; i < executionPlan.nCommit; i++) {
			store.add(new HashMap<>(sut));
		}
		blackhole.consume(sut);
		blackhole.consume(store);
	}
}
