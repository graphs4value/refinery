/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.dse.tests;

import org.junit.jupiter.api.function.Executable;
import tools.refinery.store.query.resultset.ResultSet;
import tools.refinery.store.tuple.Tuple;

import java.util.*;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertAll;

public final class QueryAssertions {
	private QueryAssertions() {
		throw new IllegalStateException("This is a static utility class and should not be instantiated directly");
	}

	public static <T> void assertNullableResults(Map<Tuple, Optional<T>> expected, ResultSet<T> resultSet) {
		var nullableValuesMap = new LinkedHashMap<Tuple, T>(expected.size());
		for (var entry : expected.entrySet()) {
			nullableValuesMap.put(entry.getKey(), entry.getValue().orElse(null));
		}
		assertResults(nullableValuesMap, resultSet);
	}

	public static <T> void assertResults(Map<Tuple, T> expected, ResultSet<T> resultSet) {
		var defaultValue = resultSet.getCanonicalQuery().defaultValue();
		var filteredExpected = new LinkedHashMap<Tuple, T>();
		var executables = new ArrayList<Executable>();
		for (var entry : expected.entrySet()) {
			var key = entry.getKey();
			var value = entry.getValue();
			if (!Objects.equals(value, defaultValue)) {
				filteredExpected.put(key, value);
			}
			executables.add(() -> assertThat("value for key " + key,resultSet.get(key), is(value)));
		}
		executables.add(() -> assertThat("results size", resultSet.size(), is(filteredExpected.size())));

		var actual = new LinkedHashMap<Tuple, T>();
		var cursor = resultSet.getAll();
		while (cursor.move()) {
			var key = cursor.getKey();
			var previous = actual.put(key, cursor.getValue());
			assertThat("duplicate value for key " + key, previous, nullValue());
		}
		executables.add(() -> assertThat("results cursor", actual, is(filteredExpected)));

		assertAll(executables);
	}
}
