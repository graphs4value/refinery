/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.query.term;

import tools.refinery.store.query.equality.LiteralEqualityHelper;
import tools.refinery.store.query.substitution.Substitution;
import tools.refinery.store.query.substitution.Substitutions;
import tools.refinery.store.query.valuation.Valuation;

import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class OpaqueTerm<T> implements Term<T> {
	private final Class<T> type;
	private final Function<? super Valuation, ? extends T> evaluator;
	private final Set<AnyDataVariable> variables;
	private final Substitution substitution;

	public OpaqueTerm(Class<T> type, Function<? super Valuation, ? extends T> evaluator,
					  Set<? extends AnyDataVariable> variables) {
		this(type, evaluator, variables, null);
	}

	private OpaqueTerm(Class<T> type, Function<? super Valuation, ? extends T> evaluator,
					   Set<? extends AnyDataVariable> variables, Substitution substitution) {
		this.type = type;
		this.evaluator = evaluator;
		this.variables = Set.copyOf(variables);
		this.substitution = substitution;
	}

	@Override
	public Class<T> getType() {
		return type;
	}

	@Override
	public Set<AnyDataVariable> getInputVariables() {
		return variables;
	}

	@Override
	public T evaluate(Valuation valuation) {
		return evaluator.apply(valuation.substitute(substitution));
	}

	@Override
	public Term<T> substitute(Substitution newSubstitution) {
		var substitutedVariables = variables.stream()
				.map(newSubstitution::getTypeSafeSubstitute)
				.collect(Collectors.toUnmodifiableSet());
		return new OpaqueTerm<>(type, evaluator, substitutedVariables,
				Substitutions.compose(substitution, newSubstitution));
	}

	@Override
	public boolean equalsWithSubstitution(LiteralEqualityHelper helper, AnyTerm other) {
		// Cannot inspect the opaque evaluator for deep equality.
		return equals(other);
	}

	@Override
	public String toString() {
		return "<opaque>";
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		OpaqueTerm<?> that = (OpaqueTerm<?>) o;
		return type.equals(that.type) && evaluator.equals(that.evaluator) && Objects.equals(substitution,
				that.substitution);
	}

	@Override
	public int hashCode() {
		return Objects.hash(type, evaluator, substitution);
	}
}
