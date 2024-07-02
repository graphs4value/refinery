/*
 * SPDX-FileCopyrightText: 2023-2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning.translator.crossreference;

import tools.refinery.logic.dnf.Query;
import tools.refinery.logic.dnf.RelationalQuery;
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
import tools.refinery.store.reasoning.translator.multiplicity.ConstrainedMultiplicity;
import tools.refinery.store.reasoning.translator.multiplicity.InvalidMultiplicityErrorTranslator;
import tools.refinery.store.reasoning.translator.multiplicity.Multiplicity;
import tools.refinery.store.representation.Symbol;

import static tools.refinery.logic.literal.Literals.not;
import static tools.refinery.store.reasoning.actions.PartialActionLiterals.add;
import static tools.refinery.store.reasoning.actions.PartialActionLiterals.remove;
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
		var sourceType = info.sourceType();
		var targetType = info.targetType();
		var defaultValue = info.defaultValue();
		if (defaultValue.must()) {
			throw new TranslationException(linkType, "Unsupported default value %s for directed cross references %s"
					.formatted(defaultValue, linkType));
		}
		var translator = PartialRelationTranslator.of(linkType);
		translator.symbol(symbol);
		if (defaultValue.may()) {
			configureWithDefaultUnknown(translator);
		} else {
			configureWithDefaultFalse(storeBuilder);
		}
		translator.refiner(DirectedCrossReferenceRefiner.of(symbol, sourceType, targetType));
		translator.initializer(new DirectedCrossReferenceInitializer(linkType, sourceType, targetType, symbol));
		if (info.partial()) {
			translator.roundingMode(RoundingMode.NONE);
		} else {
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
		}
		storeBuilder.with(translator);
		storeBuilder.with(new InvalidMultiplicityErrorTranslator(sourceType, linkType, false,
				info.sourceMultiplicity()));
		storeBuilder.with(new InvalidMultiplicityErrorTranslator(targetType, linkType, true,
				info.targetMultiplicity()));
		storeBuilder.tryGetAdapter(PropagationBuilder.class).ifPresent(this::configureLowerMultiplicityPropagator);
	}

	private void configureWithDefaultUnknown(PartialRelationTranslator translator) {
		var name = linkType.name();
		var sourceType = info.sourceType();
		var targetType = info.targetType();
		var mayNewSource = createMayHelper(sourceType, info.sourceMultiplicity(), false);
		var mayNewTarget = createMayHelper(targetType, info.targetMultiplicity(), true);
		var mayName = DnfLifter.decorateName(name, Modality.MAY, Concreteness.PARTIAL);
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
		if (info.partial()) {
			var candidateMayNewSource = createCandidateMayHelper(sourceType, info.sourceMultiplicity(), false);
			var candidateMayNewTarget = createCandidateMayHelper(targetType, info.targetMultiplicity(), true);
			var candidateMayName = DnfLifter.decorateName(name, Modality.MAY, Concreteness.CANDIDATE);
			translator.candidateMay(Query.of(candidateMayName, (builder, source, target) -> {
				builder.clause(
						candidateMayNewSource.call(source),
						candidateMayNewTarget.call(target),
						not(forbiddenView.call(source, target))
				);
				if (info.isConstrained()) {
					// Violation of monotonicity:
					// Edges violating upper multiplicity will not be marked as {@code ERROR}, but the
					// corresponding error pattern will already mark the node as invalid.
					builder.clause(
							candidateMust(linkType.call(source, target)),
							not(forbiddenView.call(source, target)),
							candidateMay(sourceType.call(source)),
							candidateMay(targetType.call(target))
					);
				}
			}));
		}
	}

	private RelationalQuery createMayHelper(PartialRelation type, Multiplicity multiplicity, boolean inverse) {
		return CrossReferenceUtils.createMayHelper(linkType, type, multiplicity, inverse);
	}

	private RelationalQuery createCandidateMayHelper(PartialRelation type, Multiplicity multiplicity,
													 boolean inverse) {
		return CrossReferenceUtils.createCandidateMayHelper(linkType, type, multiplicity, inverse);
	}

	private void configureWithDefaultFalse(ModelStoreBuilder storeBuilder) {
		var name = linkType.name();
		var sourceType = info.sourceType();
		var targetType = info.targetType();
		var mayNewSource = createMayHelper(sourceType, info.sourceMultiplicity(), false);
		var mayNewTarget = createMayHelper(targetType, info.targetMultiplicity(), true);
		storeBuilder.tryGetAdapter(PropagationBuilder.class).ifPresent(propagationBuilder -> propagationBuilder
				.rule(Rule.of(name + "#invalidLink", (builder, p1, p2) -> {
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
							remove(linkType, p1, p2)
					);
				})));
	}

	private void configureLowerMultiplicityPropagator(PropagationBuilder propagationBuilder) {
		if (info.sourceMultiplicity() instanceof ConstrainedMultiplicity constrainedMultiplicity) {
			int lowerBound = constrainedMultiplicity.multiplicity().lowerBound();
			if (lowerBound >= 1) {
				var sourceType = info.sourceType();
				CrossReferenceUtils.configureSourceLowerBound(linkType, sourceType, lowerBound, propagationBuilder);
			}
		}
		if (info.targetMultiplicity() instanceof ConstrainedMultiplicity constrainedMultiplicity) {
			int lowerBound = constrainedMultiplicity.multiplicity().lowerBound();
			if (lowerBound >= 1) {
				var targetType = info.targetType();
				CrossReferenceUtils.configureTargetLowerBound(linkType, targetType, lowerBound, propagationBuilder);
			}
		}
	}
}
