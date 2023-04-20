/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.query.dnf;

import tools.refinery.store.query.dnf.callback.*;
import tools.refinery.store.query.term.Variable;

public sealed interface Query<T> extends AnyQuery permits RelationalQuery, FunctionalQuery {
	String OUTPUT_VARIABLE_NAME = "output";

	@Override
	Class<T> valueType();

	T defaultValue();

	static QueryBuilder builder() {
		return builder(null);
	}

	static QueryBuilder builder(String name) {
		return new QueryBuilder(name);
	}

	static RelationalQuery of(QueryCallback0 callback) {
		return of(null, callback);
	}

	static RelationalQuery of(String name, QueryCallback0 callback) {
		var builder = builder(name);
		callback.accept(builder);
		return builder.build();
	}

	static RelationalQuery of(QueryCallback1 callback) {
		return of(null, callback);
	}

	static RelationalQuery of(String name, QueryCallback1 callback) {
		var builder = builder(name);
		callback.accept(builder, builder.parameter("p1"));
		return builder.build();
	}

	static RelationalQuery of(QueryCallback2 callback) {
		return of(null, callback);
	}

	static RelationalQuery of(String name, QueryCallback2 callback) {
		var builder = builder(name);
		callback.accept(builder, builder.parameter("p1"), builder.parameter("p2"));
		return builder.build();
	}

	static RelationalQuery of(QueryCallback3 callback) {
		return of(null, callback);
	}

	static RelationalQuery of(String name, QueryCallback3 callback) {
		var builder = builder(name);
		callback.accept(builder, builder.parameter("p1"), builder.parameter("p2"), builder.parameter("p3"));
		return builder.build();
	}

	static RelationalQuery of(QueryCallback4 callback) {
		return of(null, callback);
	}

	static RelationalQuery of(String name, QueryCallback4 callback) {
		var builder = builder(name);
		callback.accept(builder, builder.parameter("p1"), builder.parameter("p2"), builder.parameter("p3"),
				builder.parameter("p4"));
		return builder.build();
	}

	static <T> FunctionalQuery<T> of(Class<T> type, FunctionalQueryCallback0<T> callback) {
		return of(null, type, callback);
	}

	static <T> FunctionalQuery<T> of(String name, Class<T> type, FunctionalQueryCallback0<T> callback) {
		var outputVariable = Variable.of(OUTPUT_VARIABLE_NAME, type);
		var builder = builder(name).output(outputVariable);
		callback.accept(builder, outputVariable);
		return builder.build();
	}

	static <T> FunctionalQuery<T> of(Class<T> type, FunctionalQueryCallback1<T> callback) {
		return of(null, type, callback);
	}

	static <T> FunctionalQuery<T> of(String name, Class<T> type, FunctionalQueryCallback1<T> callback) {
		var outputVariable = Variable.of(OUTPUT_VARIABLE_NAME, type);
		var builder = builder(name).output(outputVariable);
		callback.accept(builder, builder.parameter("p1"), outputVariable);
		return builder.build();
	}

	static <T> FunctionalQuery<T> of(Class<T> type, FunctionalQueryCallback2<T> callback) {
		return of(null, type, callback);
	}

	static <T> FunctionalQuery<T> of(String name, Class<T> type, FunctionalQueryCallback2<T> callback) {
		var outputVariable = Variable.of(OUTPUT_VARIABLE_NAME, type);
		var builder = builder(name).output(outputVariable);
		callback.accept(builder, builder.parameter("p1"), builder.parameter("p2"), outputVariable);
		return builder.build();
	}

	static <T> FunctionalQuery<T> of(Class<T> type, FunctionalQueryCallback3<T> callback) {
		return of(null, type, callback);
	}

	static <T> FunctionalQuery<T> of(String name, Class<T> type, FunctionalQueryCallback3<T> callback) {
		var outputVariable = Variable.of(OUTPUT_VARIABLE_NAME, type);
		var builder = builder(name).output(outputVariable);
		callback.accept(builder, builder.parameter("p1"), builder.parameter("p2"), builder.parameter("p3"),
				outputVariable);
		return builder.build();
	}

	static <T> FunctionalQuery<T> of(Class<T> type, FunctionalQueryCallback4<T> callback) {
		return of(null, type, callback);
	}

	static <T> FunctionalQuery<T> of(String name, Class<T> type, FunctionalQueryCallback4<T> callback) {
		var outputVariable = Variable.of(OUTPUT_VARIABLE_NAME, type);
		var builder = builder(name).output(outputVariable);
		callback.accept(builder, builder.parameter("p1"), builder.parameter("p2"), builder.parameter("p3"),
				builder.parameter("p4"), outputVariable);
		return builder.build();
	}
}
