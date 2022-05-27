package tools.refinery.store.map.benchmarks.restore;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;

import java.util.concurrent.TimeUnit;

@Fork(1)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Measurement(time = 1, timeUnit = TimeUnit.SECONDS)
@Warmup(time = 1, timeUnit = TimeUnit.SECONDS)
public class ImmutableRestoreBenchmark {

	//@Benchmark
	public void immutableRestoreBenchmark(ImmutableRestoreExecutionPlan executionPlan, Blackhole blackhole) {
		var sutFilledAndCommitted = executionPlan.getSut();
		for (int i = 0; i < executionPlan.nRestore; i++) {
			sutFilledAndCommitted.restore(executionPlan.nextKey());
		}
		blackhole.consume(sutFilledAndCommitted);
	}
	
	//@Benchmark
	public void immutableRestoreBenchmarkContinuous(ImmutableRestoreExecutionPlan executionPlan, Blackhole blackhole) {
		var sutFilledAndCommitted = executionPlan.getSut();
		int nCommit = executionPlan.getnCommit();
		for (int i = 0; i < executionPlan.nRestore; i++) {
			sutFilledAndCommitted.restore(i % nCommit);
		}
		blackhole.consume(sutFilledAndCommitted);
	}

	//@Benchmark
	public void immutableRestoreAndCommitBenchmark(ImmutableRestoreExecutionPlan executionPlan, Blackhole blackhole) {
		var sutFilledAndCommitted = executionPlan.getSut();
		for (int i = 0; i < executionPlan.nRestore; i++) {
			sutFilledAndCommitted.restore(executionPlan.nextKey());
			if (i % 10 == 0) {
				blackhole.consume(sutFilledAndCommitted.commit());
			}
		}
		blackhole.consume(sutFilledAndCommitted);
	}
	
	//@Benchmark
	public void immutableRestoreAndCommitBenchmarkContinuous(ImmutableRestoreExecutionPlan executionPlan, Blackhole blackhole) {
		var sutFilledAndCommitted = executionPlan.getSut();
		int nCommit = executionPlan.getnCommit();
		for (int i = 0; i < executionPlan.nRestore; i++) {
			sutFilledAndCommitted.restore(i % nCommit);
			if (i % 10 == 0) {
				blackhole.consume(sutFilledAndCommitted.commit());
			}
		}
		blackhole.consume(sutFilledAndCommitted);
	}

	//@Benchmark
	public void baselineRestoreBenchmark(ImmutableRestoreExecutionPlan executionPlan, Blackhole blackhole) {
		var sutFilled = executionPlan.getFilledHashMap();
		var store = executionPlan.getStore();
		for (int i = 0; i < executionPlan.nRestore; i++) {
			sutFilled = store.get(executionPlan.nextKey());
		}

		blackhole.consume(sutFilled);
		blackhole.consume(store);
	}
	
	//@Benchmark
	public void baselineRestoreBenchmarkContinuous(ImmutableRestoreExecutionPlan executionPlan, Blackhole blackhole) {
		var sutFilled = executionPlan.getFilledHashMap();
		var store = executionPlan.getStore();
		int nCommit = executionPlan.getnCommit();
		for (int i = 0; i < executionPlan.nRestore; i++) {
			sutFilled = store.get(i % nCommit);
		}

		blackhole.consume(sutFilled);
		blackhole.consume(store);
	}

	//@Benchmark
	public void baselineRestoreAndCommitBenchmark(ImmutableRestoreExecutionPlan executionPlan, Blackhole blackhole) {
		var sutFilled = executionPlan.getFilledHashMap();
		var store = executionPlan.getStore();
		for (int i = 0; i < executionPlan.nRestore; i++) {
			sutFilled = store.get(executionPlan.nextKey());
			if (i % 10 == 0) {
				blackhole.consume(store.add(sutFilled));
			}
		}
		blackhole.consume(sutFilled);
		blackhole.consume(store);
	}
	
	//@Benchmark
	public void baselineRestoreAndCommitBenchmarkContinuous(ImmutableRestoreExecutionPlan executionPlan, Blackhole blackhole) {
		var sutFilled = executionPlan.getFilledHashMap();
		var store = executionPlan.getStore();
		int nCommit = executionPlan.getnCommit();
		for (int i = 0; i < executionPlan.nRestore; i++) {
			sutFilled = store.get(i % nCommit);
			if (i % 10 == 0) {
				blackhole.consume(store.add(sutFilled));
			}
		}
		blackhole.consume(sutFilled);
		blackhole.consume(store);
	}
}
