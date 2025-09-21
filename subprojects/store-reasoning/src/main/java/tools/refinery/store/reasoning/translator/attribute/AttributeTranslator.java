/*
 * SPDX-FileCopyrightText: 2025 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning.translator.attribute;

import tools.refinery.logic.AbstractValue;
import tools.refinery.store.model.ModelStoreBuilder;
import tools.refinery.store.model.ModelStoreConfiguration;
import tools.refinery.store.reasoning.representation.PartialFunction;
import tools.refinery.store.reasoning.representation.PartialRelation;
import tools.refinery.store.reasoning.translator.PartialFunctionTranslator;
import tools.refinery.store.representation.Symbol;

public class AttributeTranslator<A extends AbstractValue<A, C>, C> implements ModelStoreConfiguration {
	private final PartialRelation partialRelation;
	private final PartialFunction<A, C> partialFunction;
	private final A defaultValue;

	public AttributeTranslator(PartialFunction<A, C> partialFunction, AttributeInfo attributeInfo) {
		this.partialFunction = partialFunction;
		this.partialRelation = attributeInfo.owningType();
		if (attributeInfo.defaultValue() == null) {
			defaultValue = partialFunction.abstractDomain().unknown();
		} else {
			defaultValue = partialFunction.abstractDomain().abstractType().cast(attributeInfo.defaultValue());
		}
	}

	@Override
	public void apply(ModelStoreBuilder storeBuilder) {
		PartialFunctionTranslator<A, C> translator = PartialFunctionTranslator.of(partialFunction);
		var abstractType = partialFunction.abstractDomain().abstractType();
		Symbol<A> symbol = new Symbol<>(partialFunction.name(), partialRelation.arity(), abstractType,
				defaultValue);
		translator.symbol(symbol);
		translator.domain(partialRelation);
		storeBuilder.with(translator);
	}
}
