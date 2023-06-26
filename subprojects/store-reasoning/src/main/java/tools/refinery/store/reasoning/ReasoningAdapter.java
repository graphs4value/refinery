/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning;

import tools.refinery.store.adapter.ModelAdapter;
import tools.refinery.store.query.resultset.ResultSet;
import tools.refinery.store.query.dnf.Dnf;
import tools.refinery.store.reasoning.representation.AnyPartialSymbol;
import tools.refinery.store.reasoning.representation.PartialRelation;
import tools.refinery.store.reasoning.representation.PartialSymbol;

public interface ReasoningAdapter extends ModelAdapter {
	PartialRelation EXISTS = PartialSymbol.of("exists", 1);
	PartialRelation EQUALS = PartialSymbol.of("equals", 2);

	@Override
	ReasoningStoreAdapter getStoreAdapter();

	default AnyPartialInterpretation getPartialInterpretation(AnyPartialSymbol partialSymbol) {
		// Cast to disambiguate overloads.
		var typedPartialSymbol = (PartialSymbol<?, ?>) partialSymbol;
		return getPartialInterpretation(typedPartialSymbol);
	}

	<A, C> PartialInterpretation<A, C> getPartialInterpretation(PartialSymbol<A, C> partialSymbol);

	ResultSet<Boolean> getLiftedResultSet(Dnf query);
}
