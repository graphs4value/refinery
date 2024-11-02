/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning.translator.predicate;

import tools.refinery.logic.dnf.AbstractQueryBuilder;
import tools.refinery.logic.dnf.Dnf;
import tools.refinery.logic.dnf.Query;
import tools.refinery.logic.dnf.RelationalQuery;
import tools.refinery.logic.literal.Literal;
import tools.refinery.logic.term.NodeVariable;
import tools.refinery.logic.term.truthvalue.TruthValue;
import tools.refinery.store.dse.propagation.PropagationBuilder;
import tools.refinery.store.dse.transition.Rule;
import tools.refinery.store.model.ModelStoreBuilder;
import tools.refinery.store.model.ModelStoreConfiguration;
import tools.refinery.store.query.view.ForbiddenView;
import tools.refinery.store.query.view.MayView;
import tools.refinery.store.query.view.MustView;
import tools.refinery.store.reasoning.ReasoningAdapter;
import tools.refinery.store.reasoning.actions.PartialActionLiterals;
import tools.refinery.store.reasoning.representation.PartialRelation;
import tools.refinery.store.reasoning.translator.PartialRelationTranslator;
import tools.refinery.store.reasoning.translator.RoundingMode;
import tools.refinery.store.reasoning.translator.TranslationException;
import tools.refinery.store.reasoning.translator.TranslatorUtils;
import tools.refinery.store.representation.Symbol;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static tools.refinery.logic.literal.Literals.not;
import static tools.refinery.store.reasoning.literal.PartialLiterals.*;

public class PredicateTranslator implements ModelStoreConfiguration {
	private final PartialRelation relation;
	private final RelationalQuery query;
	private final boolean mutable;
	private final TruthValue defaultValue;
	private final List<PartialRelation> parameterTypes;
	private final Set<PartialRelation> supersets;
	private final Dnf supersetHelper;

	public PredicateTranslator(PartialRelation relation, RelationalQuery query, List<PartialRelation> parameterTypes,
							   Set<PartialRelation> supersets, boolean mutable, TruthValue defaultValue) {
		this.parameterTypes = parameterTypes;
		this.supersets = supersets;
		supersetHelper = TranslatorUtils.createSupersetHelper(relation, supersets);
		if (relation.arity() != query.arity()) {
			throw new TranslationException(relation, "Expected arity %d query for partial relation %s, got %d instead"
					.formatted(relation.arity(), relation, query.arity()));
		}
		if (defaultValue.must()) {
			throw new TranslationException(relation, "Default value must be UNKNOWN or FALSE");
		}
		this.relation = relation;
		this.query = query;
		this.mutable = mutable;
		this.defaultValue = defaultValue;
	}

	@Override
	public void apply(ModelStoreBuilder storeBuilder) {
		var translator = PartialRelationTranslator.of(relation)
				.query(query);
		if (mutable) {
			var symbol = Symbol.of(relation.name(), relation.arity(), TruthValue.class, defaultValue);
			translator.symbol(symbol);

			translator.must(Query.of(builder -> {
				var parameters = createParameters(builder);
				builder.clause(must(query.call(parameters)));
				builder.clause(new MustView(symbol).call(parameters));
			}));

			translator.may(Query.of(builder -> {
				var parameters = createParameters(builder);
				var mayLiterals = new Literal[3];
				mayLiterals[0] = may(query.call(parameters));
				if (defaultValue.may()) {
					mayLiterals[1] = not(new ForbiddenView(symbol).call(parameters));
				} else {
					mayLiterals[1] = new MayView(symbol).call(parameters);
				}
				mayLiterals[2] = may(supersetHelper.call(parameters));
				builder.clause(mayLiterals);
			}));

			supersetCandidateMay(storeBuilder, translator);

			translator.refiner(PredicateRefiner.of(symbol, parameterTypes, supersets, RoundingMode.NONE));
		} else if (defaultValue.may()) {
			if (supersets.isEmpty()) {
				// If all values are permitted, we don't need to check for any forbidden values in the model.
				// If the result of this predicate is {@code ERROR}, some other partial relation (that we check for)
				// will be {@code ERROR} as well.
				translator.exclude(null);
				translator.accept(null);
				translator.objective(null);
			} else {
				// We must enforce the superset constraint here, because there is no corresponding error pattern.
				translator.may(Query.of(builder -> {
					var parameters = createParameters(builder);
					builder.clause(
							may(query.call(parameters)),
							may(supersetHelper.call(parameters))
					);
				}));

				supersetCandidateMay(storeBuilder, translator);
			}
		} else {
			translator.mayNever();
		}
		storeBuilder.with(translator);
	}

	private NodeVariable[] createParameters(AbstractQueryBuilder<?> builder) {
		return TranslatorUtils.createParameters(relation.arity(), builder);
	}

	private void supersetCandidateMay(ModelStoreBuilder storeBuilder, PartialRelationTranslator translator) {
		if (supersets.isEmpty()) {
			return;
		}
		translator.candidateMay(Query.of(builder -> {
			var parameters = createParameters(builder);
			builder.clause(
					candidateMay(query.call(parameters)),
					candidateMay(supersetHelper.call(parameters))
			);
		}));
		storeBuilder.tryGetAdapter(PropagationBuilder.class).ifPresent(this::supersetPropagationRules);
	}

	private void supersetPropagationRules(PropagationBuilder propagationBuilder) {
		for (var superset : supersets) {
			var propagationName = relation.name() + "#propagateSuperset#" + superset.name();
			propagationBuilder.rule(Rule.of(propagationName, builder -> {
				var parameters = createParameters(builder);
				builder.clause(
						must(relation.call(parameters)),
						may(superset.call(parameters)),
						not(must(superset.call(parameters)))
				);
				builder.action(
						PartialActionLiterals.add(superset, parameters)
				);
			}));
			var concretizationName = relation.name() + "#concretizeSuperset#" + superset.name();
			propagationBuilder.concretizationRule(Rule.of(concretizationName, builder -> {
				var parameters = createParameters(builder);
				var literals = new ArrayList<Literal>(3 + parameters.length);
				literals.add(candidateMust(relation.call(parameters)));
				literals.add(candidateMay(superset.call(parameters)));
				literals.add(not(candidateMust(superset.call(parameters))));
				for (var parameter : parameters) {
					literals.add(candidateMust(ReasoningAdapter.EXISTS_SYMBOL.call(parameter)));
				}
				builder.clause(literals);
				builder.action(
						PartialActionLiterals.add(superset, parameters)
				);
			}));
		}
	}
}
