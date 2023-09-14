/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning.translator.typehierarchy;

import tools.refinery.store.dse.transition.Rule;
import tools.refinery.store.dse.transition.actions.ActionLiteral;
import tools.refinery.store.model.ModelStoreBuilder;
import tools.refinery.store.model.ModelStoreConfiguration;
import tools.refinery.store.query.dnf.Query;
import tools.refinery.store.reasoning.ReasoningBuilder;
import tools.refinery.store.reasoning.actions.PartialActionLiterals;
import tools.refinery.store.reasoning.literal.PartialLiterals;
import tools.refinery.store.reasoning.representation.PartialRelation;
import tools.refinery.store.reasoning.translator.PartialRelationTranslator;
import tools.refinery.store.reasoning.translator.multiobject.MultiObjectTranslator;
import tools.refinery.store.reasoning.translator.proxy.PartialRelationTranslatorProxy;
import tools.refinery.store.representation.Symbol;

import java.util.ArrayList;

import static tools.refinery.store.query.literal.Literals.not;
import static tools.refinery.store.reasoning.literal.PartialLiterals.candidateMust;
import static tools.refinery.store.reasoning.literal.PartialLiterals.may;

public class TypeHierarchyTranslator implements ModelStoreConfiguration {
	public static final Symbol<InferredType> TYPE_SYMBOL = Symbol.of("TYPE", 1, InferredType.class,
			InferredType.UNTYPED);
	private final TypeHierarchy typeHierarchy;

	public TypeHierarchyTranslator(TypeHierarchy typeHierarchy) {
		this.typeHierarchy = typeHierarchy;
	}

	@Override
	public void apply(ModelStoreBuilder storeBuilder) {
		if (typeHierarchy.isEmpty()) {
			return;
		}

		storeBuilder.symbol(TYPE_SYMBOL);

		for (var entry : typeHierarchy.getPreservedTypes().entrySet()) {
			storeBuilder.with(createPreservedTypeTranslator(entry.getKey(), entry.getValue()));
		}

		for (var entry : typeHierarchy.getEliminatedTypes().entrySet()) {
			storeBuilder.with(createEliminatedTypeTranslator(entry.getKey(), entry.getValue()));
		}

		var reasoningBuilder = storeBuilder.getAdapter(ReasoningBuilder.class);
		reasoningBuilder.initializer(new TypeHierarchyInitializer(typeHierarchy, TYPE_SYMBOL));
	}

	private ModelStoreConfiguration createPreservedTypeTranslator(PartialRelation type, TypeAnalysisResult result) {
		var may = Query.of(type.name() + "#partial#may", (builder, p1) -> {
			if (!result.isAbstractType()) {
				builder.clause(new MayTypeView(TYPE_SYMBOL, type).call(p1));
			}
			for (var subtype : result.getDirectSubtypes()) {
				builder.clause(may(subtype.call(p1)));
			}
		});

		var must = Query.of(type.name() + "#partial#must", (builder, p1) -> builder.clause(
				new MustTypeView(TYPE_SYMBOL, type).call(p1)
		));

		var candidate = Query.of(type.name() + "#candidate", (builder, p1) -> {
			if (!result.isAbstractType()) {
				builder.clause(new CandidateTypeView(TYPE_SYMBOL, type).call(p1));
			}
			for (var subtype : result.getDirectSubtypes()) {
				builder.clause(PartialLiterals.candidateMust(subtype.call(p1)));
			}
		});

		var translator = PartialRelationTranslator.of(type)
				.may(may)
				.must(must)
				.candidate(candidate)
				.refiner(InferredTypeRefiner.of(TYPE_SYMBOL, result));

		if (!result.isAbstractType()) {
			var decision = Rule.of(type.name(), (builder, instance) -> builder
					.clause(
							may(type.call(instance)),
							not(candidateMust(type.call(instance))),
							not(MultiObjectTranslator.MULTI_VIEW.call(instance))
					)
					.action(() -> {
						var actionLiterals = new ArrayList<ActionLiteral>();
						actionLiterals.add(PartialActionLiterals.add(type, instance));
						for (var subtype : result.getDirectSubtypes()) {
							actionLiterals.add(PartialActionLiterals.remove(subtype, instance));
						}
						return actionLiterals;
					}));
			translator.decision(decision);
		}

		return translator;
	}

	private ModelStoreConfiguration createEliminatedTypeTranslator(
			PartialRelation type, PartialRelation replacement) {
		return new PartialRelationTranslatorProxy(type, replacement, true);
	}
}
