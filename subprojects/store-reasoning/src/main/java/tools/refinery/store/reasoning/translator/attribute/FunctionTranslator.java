/*
 * SPDX-FileCopyrightText: 2025 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning.translator.attribute;

import tools.refinery.logic.AbstractValue;
import tools.refinery.logic.dnf.FunctionalQuery;
import tools.refinery.logic.dnf.Query;
import tools.refinery.logic.literal.CallPolarity;
import tools.refinery.logic.term.ConstantTerm;
import tools.refinery.logic.term.NodeVariable;
import tools.refinery.logic.term.Term;
import tools.refinery.logic.term.Variable;
import tools.refinery.logic.term.abstractdomain.AbstractDomainTerms;
import tools.refinery.store.model.ModelStoreBuilder;
import tools.refinery.store.model.ModelStoreConfiguration;
import tools.refinery.store.query.view.FunctionView;
import tools.refinery.store.reasoning.literal.ModalConstraint;
import tools.refinery.store.reasoning.literal.Modality;
import tools.refinery.store.reasoning.representation.PartialFunction;
import tools.refinery.store.reasoning.representation.PartialRelation;
import tools.refinery.store.reasoning.translator.PartialFunctionTranslator;
import tools.refinery.store.representation.Symbol;

import java.util.ArrayList;
import java.util.function.Function;

public class FunctionTranslator<A extends AbstractValue<A, C>, C> implements ModelStoreConfiguration {
	private final PartialFunction<A, C> partialFunction;
	private final PartialRelation domainRelation;
	private final FunctionalQuery<A> query;
	private final A defaultValue;
	private final boolean mutable;

	public FunctionTranslator(PartialFunction<A, C> partialFunction, PartialRelation domainRelation,
							  FunctionalQuery<A> query, A defaultValue, boolean mutable) {
		this.partialFunction = partialFunction;
		this.domainRelation = domainRelation;
		this.query = query;
		this.defaultValue = defaultValue;
		this.mutable = mutable;
	}

	@Override
	public void apply(ModelStoreBuilder storeBuilder) {
		var translator = PartialFunctionTranslator.of(partialFunction)
				.domain(domainRelation);
		var abstractDomain = partialFunction.abstractDomain();
		if (mutable) {
			var name = partialFunction.name();
			var type = abstractDomain.abstractType();
			var storageSymbol = Symbol.of(name, partialFunction.arity(), type, defaultValue);
			translator.symbol(storageSymbol);
			var mergedQuery = mergeQuery((parameters) -> new FunctionView<>(storageSymbol).leftJoin(defaultValue,
					parameters));
			translator.query(mergedQuery);
		} else if (abstractDomain.unknown().equals(defaultValue)) {
			translator.query(query);
		} else {
			var mergedQuery = mergeQuery((ignored) -> new ConstantTerm<>(abstractDomain.abstractType(), defaultValue));
			translator.query(mergedQuery);
		}
		storeBuilder.with(translator);
	}

	private FunctionalQuery<A> mergeQuery(Function<NodeVariable[], Term<A>> getTerm) {
		var name = partialFunction.name();
		var abstractDomain = partialFunction.abstractDomain();
		var type = abstractDomain.abstractType();
		int arity = partialFunction.arity();
		var parameters = new NodeVariable[arity];
		var arguments = new ArrayList<Variable>(arity + 1);
		for (int i = 0; i < arity; i++) {
			var parameter = Variable.of("p" + i);
			parameters[i] = parameter;
			arguments.add(parameter);
		}
		var computedValue = Variable.of("computedValue", type);
		arguments.add(computedValue);
		var output = Variable.of("output", type);
		return Query.builder(name)
				.parameters(parameters)
				.output(output)
				.clause(
						ModalConstraint.of(Modality.MAY, query.getDnf()).call(CallPolarity.POSITIVE, arguments),
						output.assign(AbstractDomainTerms.meet(abstractDomain, computedValue,
								getTerm.apply(parameters)))
				)
				.build();
	}
}
