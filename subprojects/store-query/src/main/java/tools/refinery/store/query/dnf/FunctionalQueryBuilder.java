/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.query.dnf;

import tools.refinery.store.query.term.DataVariable;

public final class FunctionalQueryBuilder<T> extends AbstractQueryBuilder<FunctionalQueryBuilder<T>> {
	private final DataVariable<T> outputVariable;
	private final Class<T> type;

	FunctionalQueryBuilder(DataVariable<T> outputVariable, DnfBuilder dnfBuilder, Class<T> type) {
		super(dnfBuilder);
		this.outputVariable = outputVariable;
		this.type = type;
	}

	@Override
	protected FunctionalQueryBuilder<T> self() {
		return this;
	}

	public FunctionalQuery<T> build() {
		dnfBuilder.output(outputVariable);
		return dnfBuilder.build().asFunction(type);
	}
}
