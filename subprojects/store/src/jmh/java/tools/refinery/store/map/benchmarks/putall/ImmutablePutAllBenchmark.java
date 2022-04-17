package tools.refinery.store.map.benchmarks.putall;

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
public class ImmutablePutAllBenchmark {

	@Benchmark
	public void immutablePutAllBenchmark(ImmutablePutAllExecutionPlan executionPlan, Blackhole blackhole) {
		var sut = executionPlan.createSut();
		var sutFilled = executionPlan.getSut();
		for (int i = 0; i < executionPlan.nPutAll; i++) {
			sut.putAll(sutFilled.getAll());
		}
		blackhole.consume(sut);
		blackhole.consume(sutFilled);
	}

	@Benchmark
	public void immutablePutAllAndCommitBenchmark(ImmutablePutAllExecutionPlan executionPlan, Blackhole blackhole) {
		var sut = executionPlan.createSut();
		var sutFilled = executionPlan.getSut();
		for (int i = 0; i < executionPlan.nPutAll; i++) {
			sut.putAll(sutFilled.getAll());
			if (i % 10 == 0) {
				blackhole.consume(sut.commit());
			}
		}
		blackhole.consume(sut);
		blackhole.consume(sutFilled);
	}

	@Benchmark
	public void baselinePutAllBenchmark(ImmutablePutAllExecutionPlan executionPlan, Blackhole blackhole) {
		var sutFilled = executionPlan.getFilledHashMap();
		var sut = new HashMap<Integer, String>();
		for (int i = 0; i < executionPlan.nPutAll; i++) {
			sut.putAll(sutFilled);
		}
		blackhole.consume(sutFilled);
		blackhole.consume(sut);
	}

	@Benchmark
	public void baselinePutAllAndCommitBenchmark(ImmutablePutAllExecutionPlan executionPlan, Blackhole blackhole) {
		var sutFilled = executionPlan.getFilledHashMap();
		var sut = new HashMap<Integer, String>();
		var store = new ArrayList<HashMap<Integer, String>>();
		for (int i = 0; i < executionPlan.nPutAll; i++) {
			sut.putAll(sutFilled);
			if (i % 10 == 0) {
				store.add(new HashMap<>(sut));
			}
		}
		blackhole.consume(sut);
		blackhole.consume(sutFilled);
		blackhole.consume(store);
	}
}
