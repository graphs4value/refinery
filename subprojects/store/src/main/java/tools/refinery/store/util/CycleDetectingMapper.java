/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.util;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class CycleDetectingMapper<T, R> {
	private static final String SEPARATOR = " -> ";

	private final Function<T, String> getName;

	private final Function<T, R> doMap;

	private final Set<T> inProgress = new LinkedHashSet<>();

	private final Map<T, R> results = new HashMap<>();

	public CycleDetectingMapper(Function<T, R> doMap) {
		this(Objects::toString, doMap);
	}

	public CycleDetectingMapper(Function<T, String> getName, Function<T, R> doMap) {
		this.getName = getName;
		this.doMap = doMap;
	}

	public R map(T input) {
		if (inProgress.contains(input)) {
			var path = inProgress.stream().map(getName).collect(Collectors.joining(SEPARATOR));
			throw new IllegalArgumentException("Circular reference %s%s%s detected".formatted(path, SEPARATOR,
					getName.apply(input)));
		}
		// We can't use computeIfAbsent here, because translating referenced queries calls this method in a reentrant
		// way, which would cause a ConcurrentModificationException with computeIfAbsent.
		@SuppressWarnings("squid:S3824")
		var result = results.get(input);
		if (result == null) {
			inProgress.add(input);
			try {
				result = doMap.apply(input);
				results.put(input, result);
			} finally {
				inProgress.remove(input);
			}
		}
		return result;
	}

	public List<T> getInProgress() {
		return List.copyOf(inProgress);
	}

	public R getAlreadyMapped(T input) {
		return results.get(input);
	}
}
