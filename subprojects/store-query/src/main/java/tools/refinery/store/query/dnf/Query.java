/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.query.dnf;

import tools.refinery.store.query.dnf.callback.*;
import tools.refinery.store.query.term.ParameterDirection;
import tools.refinery.store.query.term.Variable;

import java.util.Objects;

public abstract sealed class Query<T> implements AnyQuery permits FunctionalQuery, RelationalQuery {
	private static final String OUTPUT_VARIABLE_NAME = "output";

	private final Dnf dnf;

	protected Query(Dnf dnf) {
		for (var parameter : dnf.getSymbolicParameters()) {
			if (parameter.getDirection() != ParameterDirection.OUT) {
				throw new IllegalArgumentException("Query parameter %s with direction %s is not allowed"
						.formatted(parameter.getVariable(), parameter.getDirection()));
			}
		}
		this.dnf = dnf;
	}

	@Override
	public String name() {
		return dnf.name();
	}

	@Override
	public Dnf getDnf() {
		return dnf;
	}

	// Allow redeclaration of the method with refined return type.
	@SuppressWarnings("squid:S3038")
	@Override
	public abstract Class<T> valueType();

	public abstract T defaultValue();

	public Query<T> withDnf(Dnf newDnf) {
		if (dnf.equals(newDnf)) {
			return this;
		}
		int arity = dnf.arity();
		if (newDnf.arity() != arity) {
			throw new IllegalArgumentException("Arity of %s and %s do not match".formatted(dnf, newDnf));
		}
		var parameters = dnf.getParameters();
		var newParameters = newDnf.getParameters();
		for (int i = 0; i < arity; i++) {
			var parameter = parameters.get(i);
			var newParameter = newParameters.get(i);
			if (!parameter.matches(newParameter)) {
				throw new IllegalArgumentException("Parameter #%d mismatch: %s does not match %s"
						.formatted(i, parameter, newParameter));
			}
		}
		return withDnfInternal(newDnf);
	}

	protected abstract Query<T> withDnfInternal(Dnf newDnf);

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Query<?> that = (Query<?>) o;
		return Objects.equals(dnf, that.dnf);
	}

	@Override
	public int hashCode() {
		return Objects.hash(dnf);
	}

	@Override
	public String toString() {
		return dnf.toString();
	}

	public static QueryBuilder builder() {
		return builder(null);
	}

	public static QueryBuilder builder(String name) {
		return new QueryBuilder(name);
	}

	public static RelationalQuery of(QueryCallback0 callback) {
		return of(null, callback);
	}

	public static RelationalQuery of(String name, QueryCallback0 callback) {
		var builder = builder(name);
		callback.accept(builder);
		return builder.build();
	}

	public static RelationalQuery of(QueryCallback1 callback) {
		return of(null, callback);
	}

	public static RelationalQuery of(String name, QueryCallback1 callback) {
		var builder = builder(name);
		callback.accept(builder, builder.parameter("p1"));
		return builder.build();
	}

	public static RelationalQuery of(QueryCallback2 callback) {
		return of(null, callback);
	}

	public static RelationalQuery of(String name, QueryCallback2 callback) {
		var builder = builder(name);
		callback.accept(builder, builder.parameter("p1"), builder.parameter("p2"));
		return builder.build();
	}

	public static RelationalQuery of(QueryCallback3 callback) {
		return of(null, callback);
	}

	public static RelationalQuery of(String name, QueryCallback3 callback) {
		var builder = builder(name);
		callback.accept(builder, builder.parameter("p1"), builder.parameter("p2"), builder.parameter("p3"));
		return builder.build();
	}

	public static RelationalQuery of(QueryCallback4 callback) {
		return of(null, callback);
	}

	public static RelationalQuery of(String name, QueryCallback4 callback) {
		var builder = builder(name);
		callback.accept(builder, builder.parameter("p1"), builder.parameter("p2"), builder.parameter("p3"),
				builder.parameter("p4"));
		return builder.build();
	}

	public static <T> FunctionalQuery<T> of(Class<T> type, FunctionalQueryCallback0<T> callback) {
		return of(null, type, callback);
	}

	public static <T> FunctionalQuery<T> of(String name, Class<T> type, FunctionalQueryCallback0<T> callback) {
		var outputVariable = Variable.of(OUTPUT_VARIABLE_NAME, type);
		var builder = builder(name).output(outputVariable);
		callback.accept(builder, outputVariable);
		return builder.build();
	}

	public static <T> FunctionalQuery<T> of(Class<T> type, FunctionalQueryCallback1<T> callback) {
		return of(null, type, callback);
	}

	public static <T> FunctionalQuery<T> of(String name, Class<T> type, FunctionalQueryCallback1<T> callback) {
		var outputVariable = Variable.of(OUTPUT_VARIABLE_NAME, type);
		var builder = builder(name).output(outputVariable);
		callback.accept(builder, builder.parameter("p1"), outputVariable);
		return builder.build();
	}

	public static <T> FunctionalQuery<T> of(Class<T> type, FunctionalQueryCallback2<T> callback) {
		return of(null, type, callback);
	}

	public static <T> FunctionalQuery<T> of(String name, Class<T> type, FunctionalQueryCallback2<T> callback) {
		var outputVariable = Variable.of(OUTPUT_VARIABLE_NAME, type);
		var builder = builder(name).output(outputVariable);
		callback.accept(builder, builder.parameter("p1"), builder.parameter("p2"), outputVariable);
		return builder.build();
	}

	public static <T> FunctionalQuery<T> of(Class<T> type, FunctionalQueryCallback3<T> callback) {
		return of(null, type, callback);
	}

	public static <T> FunctionalQuery<T> of(String name, Class<T> type, FunctionalQueryCallback3<T> callback) {
		var outputVariable = Variable.of(OUTPUT_VARIABLE_NAME, type);
		var builder = builder(name).output(outputVariable);
		callback.accept(builder, builder.parameter("p1"), builder.parameter("p2"), builder.parameter("p3"),
				outputVariable);
		return builder.build();
	}

	public static <T> FunctionalQuery<T> of(Class<T> type, FunctionalQueryCallback4<T> callback) {
		return of(null, type, callback);
	}

	public static <T> FunctionalQuery<T> of(String name, Class<T> type, FunctionalQueryCallback4<T> callback) {
		var outputVariable = Variable.of(OUTPUT_VARIABLE_NAME, type);
		var builder = builder(name).output(outputVariable);
		callback.accept(builder, builder.parameter("p1"), builder.parameter("p2"), builder.parameter("p3"),
				builder.parameter("p4"), outputVariable);
		return builder.build();
	}
}
