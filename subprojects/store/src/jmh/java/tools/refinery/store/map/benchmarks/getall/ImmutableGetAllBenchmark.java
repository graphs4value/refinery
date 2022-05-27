package tools.refinery.store.map.benchmarks.getall;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;

import tools.refinery.store.map.Cursor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.concurrent.TimeUnit;

@Fork(1)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Measurement(time = 1, timeUnit = TimeUnit.SECONDS)
@Warmup(time = 1, timeUnit = TimeUnit.SECONDS)
public class ImmutableGetAllBenchmark {
	

	@Benchmark
	public void immutableGetAllBenchmark(ImmutableGetAllExecutionPlan executionPlan, Blackhole blackhole) {
		var sut = executionPlan.getSut();
		for (int i = 0; i < executionPlan.nGetAll; i++) {
			Cursor<Integer, String> cursor = sut.getAll();
			while(cursor.move())  {				
				blackhole.consume(cursor.getKey());
			}
		}
	}

	@Benchmark
	public void immutableGetAllAndCommitBenchmark(ImmutableGetAllExecutionPlan executionPlan, Blackhole blackhole) {
		var sut = executionPlan.getSut();
		for (int i = 0; i < executionPlan.nGetAll; i++) {
			Cursor<Integer, String> cursor = sut.getAll();
			while(cursor.move())  {	
				blackhole.consume(cursor.getKey());
			}
			if (i % 10 == 0) {
				blackhole.consume(sut.commit());
			}
		}
	}

	@Benchmark
	public void baselineGetAllValuesBenchmark(ImmutableGetAllExecutionPlan executionPlan, Blackhole blackhole) {
		var hashMap = executionPlan.getFilledHashMap();
		for (int i = 0; i < executionPlan.nGetAll; i++) {
			Iterator<Integer> keys = hashMap.keySet().iterator();
			while (keys.hasNext()) {
			    blackhole.consume(keys.next());
			}
		}
		
	}

	@Benchmark
	public void baselineGetAllValuesAndCommitBenchmark(ImmutableGetAllExecutionPlan executionPlan, Blackhole blackhole) {
		var hashMap = executionPlan.getFilledHashMap();
		var store = new ArrayList<HashMap<Integer, String>>();
		for (int i = 0; i < executionPlan.nGetAll; i++) {
			//Collection<String> values = hashMap.values();
			Iterator<Integer> keys = hashMap.keySet().iterator();
			while (keys.hasNext()) {
				    blackhole.consume(keys.next());
			}
			if (i % 10 == 0) {
				blackhole.consume(store.add(new HashMap<>(hashMap)));
			}
		}
		
	}
}
