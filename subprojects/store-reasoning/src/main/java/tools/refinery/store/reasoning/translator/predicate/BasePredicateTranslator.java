/*
 * SPDX-FileCopyrightText: 2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning.translator.predicate;

import tools.refinery.logic.dnf.AbstractQueryBuilder;
import tools.refinery.logic.dnf.Query;
import tools.refinery.logic.literal.Literal;
import tools.refinery.logic.term.NodeVariable;
import tools.refinery.logic.term.truthvalue.TruthValue;
import tools.refinery.store.dse.propagation.PropagationBuilder;
import tools.refinery.store.dse.transition.Rule;
import tools.refinery.store.model.ModelStoreBuilder;
import tools.refinery.store.model.ModelStoreConfiguration;
import tools.refinery.store.query.view.ForbiddenView;
import tools.refinery.store.reasoning.lifting.DnfLifter;
import tools.refinery.store.reasoning.literal.Concreteness;
import tools.refinery.store.reasoning.literal.Modality;
import tools.refinery.store.reasoning.representation.PartialRelation;
import tools.refinery.store.reasoning.translator.PartialRelationTranslator;
import tools.refinery.store.reasoning.translator.RoundingMode;
import tools.refinery.store.reasoning.translator.TranslationException;
import tools.refinery.store.representation.Symbol;

import java.util.ArrayList;
import java.util.List;

import static tools.refinery.logic.literal.Literals.not;
import static tools.refinery.store.reasoning.actions.PartialActionLiterals.add;
import static tools.refinery.store.reasoning.literal.PartialLiterals.*;
import static tools.refinery.store.reasoning.translator.multiobject.MultiObjectTranslator.MULTI_VIEW;

public class BasePredicateTranslator implements ModelStoreConfiguration {
	private final PartialRelation predicate;
	private final List<PartialRelation> parameterTypes;
	private final TruthValue defaultValue;
	private final boolean partial;
	private final Symbol<TruthValue> symbol;

	public BasePredicateTranslator(PartialRelation predicate, List<PartialRelation> parameterTypes,
								   TruthValue defaultValue, boolean partial) {
		this.predicate = predicate;
		this.parameterTypes = parameterTypes;
		this.defaultValue = defaultValue;
		this.partial = partial;
		symbol = Symbol.of(predicate.name(), predicate.arity(), TruthValue.class, defaultValue);
	}

	@Override
	public void apply(ModelStoreBuilder storeBuilder) {
		int arity = predicate.arity();
		if (arity != parameterTypes.size()) {
			throw new TranslationException(predicate,
					"Expected %d parameter type for base predicate %s, got %d instead"
							.formatted(arity, predicate, parameterTypes.size()));
		}
		if (defaultValue.must()) {
			throw new TranslationException(predicate, "Unsupported default value %s for base predicate %s"
					.formatted(defaultValue, predicate));
		}
		var translator = PartialRelationTranslator.of(predicate);
		translator.symbol(symbol);
		if (defaultValue.may()) {
			configureWithDefaultUnknown(translator);
		} else {
			configureWithDefaultFalse(storeBuilder);
		}
		translator.refiner(PredicateRefiner.of(symbol, parameterTypes));
		if (partial) {
			translator.roundingMode(RoundingMode.NONE);
		} else {
			translator.decision(Rule.of(predicate.name(), builder -> {
				var parameters = createParameters(builder);
				var literals = new ArrayList<Literal>(arity + 2);
				literals.add(may(predicate.call(parameters)));
				literals.add(not(candidateMust(predicate.call(parameters))));
				for (int i = 0; i < arity; i++) {
					literals.add(not(MULTI_VIEW.call(parameters[i])));
				}
				builder.clause(literals);
				builder.action(add(predicate, parameters));
			}));
		}
		storeBuilder.with(translator);
	}

	private NodeVariable[] createParameters(AbstractQueryBuilder<?> builder) {
		int arity = predicate.arity();
		var parameters = new NodeVariable[arity];
		for (int i = 0; i < arity; i++) {
			parameters[i] = builder.parameter("p" + (i + 1));
		}
		return parameters;
	}

	private void configureWithDefaultUnknown(PartialRelationTranslator translator) {
		var name = predicate.name();
		var mayName = DnfLifter.decorateName(name, Modality.MAY, Concreteness.PARTIAL);
		int arity = predicate.arity();
		var forbiddenView = new ForbiddenView(symbol);
		translator.may(Query.of(mayName, builder -> {
			var parameters = createParameters(builder);
			var literals = new ArrayList<Literal>(arity + 1);
			for (int i = 0; i < arity; i++) {
				literals.add(may(parameterTypes.get(i).call(parameters[i])));
			}
			literals.add(not(forbiddenView.call(parameters)));
			builder.clause(literals);
		}));
		if (partial) {
			var candidateMayName = DnfLifter.decorateName(name, Modality.MAY, Concreteness.CANDIDATE);
			translator.candidateMay(Query.of(candidateMayName, builder -> {
				var parameters = createParameters(builder);
				var literals = new ArrayList<Literal>(arity + 1);
				for (int i = 0; i < arity; i++) {
					literals.add(candidateMay(parameterTypes.get(i).call(parameters[i])));
				}
				literals.add(not(forbiddenView.call(parameters)));
				builder.clause(literals);
			}));
		}
	}

	private void configureWithDefaultFalse(ModelStoreBuilder storeBuilder) {
		var name = predicate.name();
		// Fail if there is no {@link PropagationBuilder}, since it is required for soundness.
		var propagationBuilder = storeBuilder.getAdapter(PropagationBuilder.class);
		propagationBuilder.rule(Rule.of(name + "#invalidLink", builder -> {
			var parameters = createParameters(builder);
			int arity = parameters.length;
			for (int i = 0; i < arity; i++) {
				builder.clause(
						may(predicate.call(parameters)),
						not(may(parameterTypes.get(i).call(parameters[i])))
				);
			}
		}));
	}
}
