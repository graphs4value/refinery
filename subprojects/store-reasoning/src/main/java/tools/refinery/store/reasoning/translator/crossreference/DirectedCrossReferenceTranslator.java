/*
 * SPDX-FileCopyrightText: 2023-2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning.translator.crossreference;

import tools.refinery.logic.dnf.Dnf;
import tools.refinery.logic.dnf.Query;
import tools.refinery.logic.dnf.RelationalQuery;
import tools.refinery.logic.literal.Literal;
import tools.refinery.logic.term.ParameterDirection;
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
import tools.refinery.store.reasoning.translator.multiplicity.InvalidMultiplicityErrorTranslator;
import tools.refinery.store.reasoning.translator.multiplicity.Multiplicity;
import tools.refinery.store.representation.Symbol;

import java.util.ArrayList;

import static tools.refinery.logic.literal.Literals.not;
import static tools.refinery.store.reasoning.actions.PartialActionLiterals.add;
import static tools.refinery.store.reasoning.actions.PartialActionLiterals.remove;
import static tools.refinery.store.reasoning.literal.PartialLiterals.*;
import static tools.refinery.store.reasoning.translator.multiobject.MultiObjectTranslator.MULTI_VIEW;

public class DirectedCrossReferenceTranslator implements ModelStoreConfiguration {
	private final PartialRelation linkType;
	private final DirectedCrossReferenceInfo info;
	private final Symbol<TruthValue> symbol;

	//Konstruktor. Beállítja a linktypeot, az infot symbol?
	public DirectedCrossReferenceTranslator(PartialRelation linkType, DirectedCrossReferenceInfo info) {
		this.linkType = linkType;
		this.info = info;
		symbol = Symbol.of(linkType.name(), 2, TruthValue.class, info.defaultValue());
	}

	@Override
	public void apply(ModelStoreBuilder storeBuilder) {
		//A sourceType és a targetType a DirectedCrossReferenceInfo-ból származik ahogy a defaultValue is.
		var sourceType = info.sourceType();
		var targetType = info.targetType();
		var defaultValue = info.defaultValue();
		//Ha a defaultValue igaz vagy error akkor exceptiont dob.
		if (defaultValue.must()) {
			throw new TranslationException(linkType, "Unsupported default value %s for directed cross reference %s"
					.formatted(defaultValue, linkType));
		}
		//A PartialRelation linkTypeból PartialRelationTranslator-t csinál.
		var translator = PartialRelationTranslator.of(linkType);
		//A symbolt beállítja a translatoron.
		translator.symbol(symbol);
		//Ha a defaultValue unknown akkor a configureWithDefaultUnknown-t hívja meg a translatoron.
		if (defaultValue.may()) {
			configureWithDefaultUnknown(translator);
		} else {
			configureWithDefaultFalse(storeBuilder);
		}
		translator.refiner(DirectedCrossReferenceRefiner.of(symbol, sourceType, targetType, info.supersets(),
				info.oppositeSupersets()));
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
	}

	private void configureWithDefaultUnknown(PartialRelationTranslator translator) {
		//Beállítja a partial relation namejét, source és target typeját
		var name = linkType.name();
		var sourceType = info.sourceType();
		var targetType = info.targetType();
		//Létrehoz egy mayNewSource és mayNewTargetet a sourceType és targetType alapján.
		var mayNewSource = createMayHelper(sourceType, info.sourceMultiplicity(), false);
		var mayNewTarget = createMayHelper(targetType, info.targetMultiplicity(), true);
		var superset = createSupersetHelper();
		//Ez csak fancy nevet ad neki az infókból string formájában.
		var mayName = DnfLifter.decorateName(name, Modality.MAY, Concreteness.PARTIAL);
		var forbiddenView = new ForbiddenView(symbol);
		translator.may(Query.of(mayName, (builder, source, target) -> {
			builder.clause(
					may(superset.call(source, target)),
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
						may(superset.call(source, target)),
						//negálás
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
						candidateMay(superset.call(source, target)),
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
							candidateMay(superset.call(source, target)),
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

	private Dnf createSupersetHelper() {
		int supersetCount = info.supersets().size();
		int oppositeSupersetCount = info.oppositeSupersets().size();
		int literalCount = supersetCount + oppositeSupersetCount;
		var direction = literalCount >= 1 ? ParameterDirection.OUT : ParameterDirection.IN;
		return Dnf.of(linkType.name() + "#superset", (builder) -> {
			var p1 = builder.parameter("p1", direction);
			var p2 = builder.parameter("p2", direction);
			var literals = new ArrayList<Literal>(literalCount);
			for (PartialRelation superset : info.supersets()) {
				literals.add(superset.call(p1, p2));
			}
			for (PartialRelation oppositeSuperset : info.oppositeSupersets()) {
				literals.add(oppositeSuperset.call(p2, p1));
			}
			builder.clause(literals);
		});
	}

	private void configureWithDefaultFalse(ModelStoreBuilder storeBuilder) {
		var name = linkType.name();
		var sourceType = info.sourceType();
		var targetType = info.targetType();
		var mayNewSource = createMayHelper(sourceType, info.sourceMultiplicity(), false);
		var mayNewTarget = createMayHelper(targetType, info.targetMultiplicity(), true);
		var superset = createSupersetHelper();
		// Fail if there is no {@link PropagationBuilder}, since it is required for soundness.
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
			builder.clause(
					may(linkType.call(p1, p2)),
					not(may(superset.call(p1, p2)))
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
		}));
	}
}
