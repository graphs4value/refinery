/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.query.dnf;

import tools.refinery.store.query.term.DataVariable;

public final class QueryBuilder extends AbstractQueryBuilder<QueryBuilder> {
	QueryBuilder(String name) {
		super(Dnf.builder(name));
	}

	@Override
	protected QueryBuilder self() {
		return this;
	}

	public <T> FunctionalQueryBuilder<T> output(DataVariable<T> outputVariable) {
		return new FunctionalQueryBuilder<>(outputVariable, dnfBuilder, outputVariable.getType());
	}

	public RelationalQuery build() {
		return dnfBuilder.build().asRelation();
	}
}
