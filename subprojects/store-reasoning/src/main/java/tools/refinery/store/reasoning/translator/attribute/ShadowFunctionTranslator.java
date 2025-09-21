/*
 * SPDX-FileCopyrightText: 2025 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning.translator.attribute;

import tools.refinery.logic.AbstractValue;
import tools.refinery.logic.dnf.FunctionalQuery;
import tools.refinery.store.model.ModelStoreBuilder;
import tools.refinery.store.model.ModelStoreConfiguration;
import tools.refinery.store.reasoning.representation.PartialFunction;
import tools.refinery.store.reasoning.representation.PartialRelation;
import tools.refinery.store.reasoning.translator.MissingInterpretation;
import tools.refinery.store.reasoning.translator.PartialFunctionTranslator;

public class ShadowFunctionTranslator<A extends AbstractValue<A, C>, C> implements ModelStoreConfiguration {
	private final PartialFunction<A, C> partialFunction;
	private final PartialRelation domainRelation;
	private final FunctionalQuery<A> query;
	private final boolean hasInterpretation;

	public ShadowFunctionTranslator(PartialFunction<A, C> partialFunction, PartialRelation domainRelation,
									FunctionalQuery<A> query, boolean hasInterpretation) {
		this.partialFunction = partialFunction;
		this.domainRelation = domainRelation;
		this.query = query;
		this.hasInterpretation = hasInterpretation;
	}

	@Override
	public void apply(ModelStoreBuilder storeBuilder) {
		var translator = PartialFunctionTranslator.of(partialFunction)
				.domain(domainRelation)
				.query(query)
				// We don't care if a shadow function is {@code error}.
				.objective(null)
				.exclude(null)
				.accept(null);
		if (!hasInterpretation) {
			translator.interpretation(MissingInterpretation::new);
		}
		storeBuilder.with(translator);
	}
}
