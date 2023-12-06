/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning.translator.crossreference;

import tools.refinery.store.dse.propagation.PropagationBuilder;
import tools.refinery.store.dse.transition.Rule;
import tools.refinery.store.model.ModelStoreBuilder;
import tools.refinery.store.model.ModelStoreConfiguration;
import tools.refinery.store.query.dnf.Query;
import tools.refinery.store.query.dnf.RelationalQuery;
import tools.refinery.store.query.view.ForbiddenView;
import tools.refinery.store.reasoning.lifting.DnfLifter;
import tools.refinery.store.reasoning.literal.Concreteness;
import tools.refinery.store.reasoning.literal.Modality;
import tools.refinery.store.reasoning.representation.PartialRelation;
import tools.refinery.store.reasoning.translator.PartialRelationTranslator;
import tools.refinery.store.reasoning.translator.TranslationException;
import tools.refinery.store.reasoning.translator.multiplicity.InvalidMultiplicityErrorTranslator;
import tools.refinery.store.reasoning.translator.multiplicity.Multiplicity;
import tools.refinery.store.representation.Symbol;
import tools.refinery.store.representation.TruthValue;

import static tools.refinery.store.query.literal.Literals.not;
import static tools.refinery.store.reasoning.actions.PartialActionLiterals.add;
import static tools.refinery.store.reasoning.actions.PartialActionLiterals.merge;
import static tools.refinery.store.reasoning.literal.PartialLiterals.*;
import static tools.refinery.store.reasoning.translator.multiobject.MultiObjectTranslator.MULTI_VIEW;

public class DirectedCrossReferenceTranslator implements ModelStoreConfiguration {
	private final PartialRelation linkType;
	private final DirectedCrossReferenceInfo info;
	private final Symbol<TruthValue> symbol;

	public DirectedCrossReferenceTranslator(PartialRelation linkType, DirectedCrossReferenceInfo info) {
		this.linkType = linkType;
		this.info = info;
		symbol = Symbol.of(linkType.name(), 2, TruthValue.class, info.defaultValue());
	}

	@Override
	public void apply(ModelStoreBuilder storeBuilder) {
		var name = linkType.name();
		var sourceType = info.sourceType();
		var targetType = info.targetType();
		var mayNewSource = createMayHelper(sourceType, info.sourceMultiplicity(), false);
		var mayNewTarget = createMayHelper(targetType, info.targetMultiplicity(), true);
		var mayName = DnfLifter.decorateName(name, Modality.MAY, Concreteness.PARTIAL);

		var defaultValue = info.defaultValue();
		if (defaultValue.must()) {
			throw new TranslationException(linkType, "Unsupported default value %s for directed cross references %s"
					.formatted(defaultValue, linkType));
		}

		var translator = PartialRelationTranslator.of(linkType);
		translator.symbol(symbol);
		if (defaultValue.may()) {
			var forbiddenView = new ForbiddenView(symbol);
			translator.may(Query.of(mayName, (builder, source, target) -> {
				builder.clause(
						mayNewSource.call(source),
						mayNewTarget.call(target),
						not(forbiddenView.call(source, target))
				);
				if (info.isConstrained()) {
					// Violation of monotonicity:
					// Edges violating upper multiplicity will not be marked as {@code ERROR}, but the
					// corresponding error pattern will already mark the node as invalid.
					builder.clause(
							must(linkType.call(source, target)),
							not(forbiddenView.call(source, target)),
							may(sourceType.call(source)),
							may(targetType.call(target))
					);
				}
			}));
		} else {
			var propagationBuilder = storeBuilder.getAdapter(PropagationBuilder.class);
			propagationBuilder.rule(Rule.of(name + "#invalidLink", (builder, p1, p2) -> {
				builder.clause(
						may(linkType.call(p1, p2)),
						not(may(sourceType.call(p1)))
				);
				builder.clause(
						may(linkType.call(p1, p2)),
						not(may(targetType.call(p2)))
				);
				if (info.isConstrained()) {
					builder.clause(
							may(linkType.call(p1, p2)),
							not(must(linkType.call(p1, p2))),
							not(mayNewSource.call(p1))
					);
					builder.clause(
							may(linkType.call(p1, p2)),
							not(must(linkType.call(p1, p2))),
							not(mayNewTarget.call(p2))
					);
				}
				builder.action(
						merge(linkType, TruthValue.FALSE, p1, p2)
				);
			}));
		}
		translator.refiner(DirectedCrossReferenceRefiner.of(symbol, sourceType, targetType));
		translator.initializer(new DirectedCrossReferenceInitializer(linkType, sourceType, targetType, symbol));
		translator.decision(Rule.of(linkType.name(), (builder, source, target) -> builder
				.clause(
						may(linkType.call(source, target)),
						not(candidateMust(linkType.call(source, target))),
						not(MULTI_VIEW.call(source)),
						not(MULTI_VIEW.call(target))
				)
				.action(
						add(linkType, source, target)
				)));
		storeBuilder.with(translator);

		storeBuilder.with(new InvalidMultiplicityErrorTranslator(sourceType, linkType, false,
				info.sourceMultiplicity()));

		storeBuilder.with(new InvalidMultiplicityErrorTranslator(targetType, linkType, true,
				info.targetMultiplicity()));
	}

	private RelationalQuery createMayHelper(PartialRelation type, Multiplicity multiplicity, boolean inverse) {
		return CrossReferenceUtils.createMayHelper(linkType, type, multiplicity, inverse);
	}
}
