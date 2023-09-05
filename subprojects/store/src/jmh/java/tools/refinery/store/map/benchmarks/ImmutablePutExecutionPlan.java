/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.map.benchmarks;

import java.util.Objects;
import java.util.Random;

import tools.refinery.store.map.ContinuousHashProvider;
import tools.refinery.store.map.VersionedMapStore;
import tools.refinery.store.map.internal.state.VersionedMapStoreStateImpl;
import tools.refinery.store.map.internal.state.VersionedMapStateImpl;
import tools.refinery.store.map.tests.utils.MapTestEnvironment;

import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;

@State(Scope.Benchmark)
public class ImmutablePutExecutionPlan {

	@Param({ "100", "10000" })
	public int nPut;

	@Param({ "32", "1000", "100000" })
	public int nKeys;

	@Param({ "2", "3" })
	public int nValues;

	private Random random;

	private String[] values;

	private ContinuousHashProvider<Integer> hashProvider = MapTestEnvironment.prepareHashProvider(false);

	@Setup(Level.Trial)
	public void setUpTrial() {
		random = new Random();
		values = MapTestEnvironment.prepareValues(nValues, true);
	}

	public VersionedMapStateImpl<Integer, String> createSut() {
		VersionedMapStore<Integer, String> store = new VersionedMapStoreStateImpl<>(hashProvider, values[0]);
		return (VersionedMapStateImpl<Integer, String>) store.createMap();
	}

	public Integer nextKey() {
		return random.nextInt(nKeys);
	}

	public boolean isDefault(String value) {
		return Objects.equals(value,values[0]);
	}

	public String nextValue() {
		return values[random.nextInt(nValues)];
	}
}
