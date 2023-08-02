/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.map.tests.fuzz;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import tools.refinery.store.map.Version;
import tools.refinery.store.map.VersionedMap;
import tools.refinery.store.map.VersionedMapStore;
import tools.refinery.store.map.tests.utils.MapTestEnvironment;

public class MultiThreadTestRunnable implements Runnable {
	final String scenario;
	final VersionedMapStore<Integer, String> store;
	final int steps;
	final int maxKey;
	final String[] values;
	final int seed;
	final int commitFrequency;
	final List<Throwable> errors = new LinkedList<>();

	public MultiThreadTestRunnable(String scenario, VersionedMapStore<Integer, String> store, int steps,
								   int maxKey, String[] values, int seed, int commitFrequency) {
		super();
		this.scenario = scenario;
		this.store = store;
		this.steps = steps;
		this.maxKey = maxKey;
		this.values = values;
		this.seed = seed;
		this.commitFrequency = commitFrequency;
	}

	private void logAndThrowError(String message) {
		AssertionError error = new AssertionError(message);
		errors.add(error);
	}

	public List<Throwable> getErrors() {
		return errors;
	}

	@Override
	public void run() {
		try{
			task();
		} catch(Exception e) {
			e.printStackTrace();
		}
	}

	private void task() {
		// 1. build a map with versions
		Random r = new Random(seed);
		VersionedMap<Integer, String> versioned =  store.createMap();
		Map<Integer, Version> index2Version = new HashMap<>();

		for (int i = 0; i < steps; i++) {
			int index = i + 1;
			int nextKey = r.nextInt(maxKey);
			String nextValue = values[r.nextInt(values.length)];
			try {
				versioned.put(nextKey, nextValue);
			} catch (Exception exception) {
				exception.printStackTrace();
				logAndThrowError(scenario + ":" + index + ": exception happened: " + exception);
			}
			if (index % commitFrequency == 0) {
				Version version = versioned.commit();
				index2Version.put(i, version);
			}
			MapTestEnvironment.printStatus(scenario, index, steps, "building");
		}
		// 2. create a non-versioned
		VersionedMap<Integer, String> reference = store.createMap();
		r = new Random(seed);
		Random r2 = new Random(seed + 1);

		for (int i = 0; i < steps; i++) {
			int index = i + 1;
			int nextKey = r.nextInt(maxKey);
			String nextValue = values[r.nextInt(values.length)];
			try {
				reference.put(nextKey, nextValue);
			} catch (Exception exception) {
				exception.printStackTrace();
				logAndThrowError(scenario + ":" + index + ": exception happened: " + exception);
			}
			// go back to an existing state and compare to the reference
			if (index % (commitFrequency) == 0) {
				versioned.restore(index2Version.get(i));
				MapTestEnvironment.compareTwoMaps(scenario + ":" + index, reference, versioned, null);

				// go back to a random state (probably created by another thread)
				List<Version> states = new ArrayList<>(index2Version.values());
				//states.sort(Long::compare);
				Collections.shuffle(states, r2);
				for (Version state : states.subList(0, Math.min(states.size(), 100))) {
					versioned.restore(state);
					var clean = store.createMap(state);
					MapTestEnvironment.compareTwoMaps(scenario + ":" + index, clean, versioned, null);
				}
				versioned.restore(index2Version.get(i));
			}

			MapTestEnvironment.printStatus(scenario, index, steps, "comparison");
		}
	}
}
