/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.query.dnf;

import tools.refinery.store.query.literal.Literal;
import tools.refinery.store.query.term.Variable;

import java.util.Collection;
import java.util.Set;

public final class FunctionalQueryBuilder<T> {
	private final DnfBuilder dnfBuilder;
	private final Class<T> type;

	FunctionalQueryBuilder(DnfBuilder dnfBuilder, Class<T> type) {
		this.dnfBuilder = dnfBuilder;
		this.type = type;
	}

	public FunctionalQueryBuilder<T> functionalDependencies(Collection<FunctionalDependency<Variable>> functionalDependencies) {
		dnfBuilder.functionalDependencies(functionalDependencies);
		return this;
	}

	public FunctionalQueryBuilder<T> functionalDependency(FunctionalDependency<Variable> functionalDependency) {
		dnfBuilder.functionalDependency(functionalDependency);
		return this;
	}

	public FunctionalQueryBuilder<T> functionalDependency(Set<? extends Variable> forEach, Set<? extends Variable> unique) {
		dnfBuilder.functionalDependency(forEach, unique);
		return this;
	}

	public FunctionalQueryBuilder<T> clause(Literal... literals) {
		dnfBuilder.clause(literals);
		return this;
	}

	public FunctionalQueryBuilder<T> clause(Collection<? extends Literal> literals) {
		dnfBuilder.clause(literals);
		return this;
	}

	public FunctionalQuery<T> build() {
		return dnfBuilder.build().asFunction(type);
	}
}
