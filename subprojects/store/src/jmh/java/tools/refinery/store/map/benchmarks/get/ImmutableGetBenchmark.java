package tools.refinery.store.map.benchmarks.get;

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
public class ImmutableGetBenchmark {

	@Benchmark
	public void immutableGetBenchmark(ImmutableGetExecutionPlan executionPlan, Blackhole blackhole) {
		var sut = executionPlan.getSut();
		for (int i = 0; i < executionPlan.nGet; i++) {
			blackhole.consume(sut.get(executionPlan.nextKey()));
		}
		
	}
	
	@Benchmark
	public void immutableGetBenchmarkContinuous(ImmutableGetExecutionPlan executionPlan, Blackhole blackhole) {
		var sut = executionPlan.getSut();
		for (int i = 0; i < executionPlan.nGet; i++) {
			blackhole.consume(sut.get(i % executionPlan.nKeys));
		}
		
	}
	
	@Benchmark
	public void immutableGetAndCommitBenchmark(ImmutableGetExecutionPlan executionPlan, Blackhole blackhole) {
		var sut = executionPlan.getSut();
		for (int i = 0; i < executionPlan.nGet; i++) {
			blackhole.consume(sut.get(executionPlan.nextKey()));
			if (i % 10 == 0) {
				blackhole.consume(sut.commit());
			}
		}
		
	}
	
	@Benchmark
	public void immutableGetAndCommitBenchmarkContinuous(ImmutableGetExecutionPlan executionPlan, Blackhole blackhole) {
		var sut = executionPlan.getSut();
		for (int i = 0; i < executionPlan.nGet; i++) {
			blackhole.consume(sut.get(i % executionPlan.nKeys));
			if (i % 10 == 0) {
				blackhole.consume(sut.commit());
			}
		}
		
	}

	@Benchmark
	public void baselineGetBenchmark(ImmutableGetExecutionPlan executionPlan, Blackhole blackhole) {
		var hashMap = executionPlan.getFilledHashMap();
		for (int i = 0; i < executionPlan.nGet; i++) {
			blackhole.consume(hashMap.get(executionPlan.nextKey()));
		}
	}
	
	@Benchmark
	public void baselineGetBenchmarkContinuous(ImmutableGetExecutionPlan executionPlan, Blackhole blackhole) {
		var hashMap = executionPlan.getFilledHashMap();
		for (int i = 0; i < executionPlan.nGet; i++) {
			blackhole.consume(hashMap.get(i % executionPlan.nKeys));
		}
	}

	@Benchmark
	public void baselineGetAndCommitBenchmark(ImmutableGetExecutionPlan executionPlan, Blackhole blackhole) {
		var hashMap = executionPlan.getFilledHashMap();
		var store = new ArrayList<HashMap<Integer, String>>();
		for (int i = 0; i < executionPlan.nGet; i++) {
			blackhole.consume(hashMap.get(executionPlan.nextKey()));
			if (i % 10 == 0) {
				blackhole.consume(store.add(new HashMap<>(hashMap)));
			}
		}
	}
	
	@Benchmark
	public void baselineGetAndCommitBenchmarkContinuous(ImmutableGetExecutionPlan executionPlan, Blackhole blackhole) {
		var hashMap = executionPlan.getFilledHashMap();
		var store = new ArrayList<HashMap<Integer, String>>();
		for (int i = 0; i < executionPlan.nGet; i++) {
			blackhole.consume(hashMap.get(i % executionPlan.nKeys));
			if (i % 10 == 0) {
				blackhole.consume(store.add(new HashMap<>(hashMap)));
			}
		}
	}
}
