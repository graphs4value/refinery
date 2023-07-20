/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning.translator.typehierarchy;

import tools.refinery.store.model.ModelStoreBuilder;
import tools.refinery.store.model.ModelStoreConfiguration;
import tools.refinery.store.query.dnf.Query;
import tools.refinery.store.reasoning.ReasoningBuilder;
import tools.refinery.store.reasoning.literal.PartialLiterals;
import tools.refinery.store.reasoning.representation.PartialRelation;
import tools.refinery.store.reasoning.translator.PartialRelationTranslator;
import tools.refinery.store.reasoning.translator.proxy.PartialRelationTranslatorProxy;
import tools.refinery.store.representation.Symbol;

public class TypeHierarchyTranslator implements ModelStoreConfiguration {
	private final Symbol<InferredType> typeSymbol = Symbol.of("TYPE", 1, InferredType.class, InferredType.UNTYPED);
	private final TypeHierarchy typeHierarchy;

	public TypeHierarchyTranslator(TypeHierarchy typeHierarchy) {
		this.typeHierarchy = typeHierarchy;
	}

	@Override
	public void apply(ModelStoreBuilder storeBuilder) {
		if (typeHierarchy.isEmpty()) {
			return;
		}

		storeBuilder.symbol(typeSymbol);

		for (var entry : typeHierarchy.getPreservedTypes().entrySet()) {
			storeBuilder.with(createPreservedTypeTranslator(entry.getKey(), entry.getValue()));
		}

		for (var entry : typeHierarchy.getEliminatedTypes().entrySet()) {
			storeBuilder.with(createEliminatedTypeTranslator(entry.getKey(), entry.getValue()));
		}

		var reasoningBuilder = storeBuilder.getAdapter(ReasoningBuilder.class);
		reasoningBuilder.initializer(new TypeHierarchyInitializer(typeHierarchy, typeSymbol));
	}

	private ModelStoreConfiguration createPreservedTypeTranslator(PartialRelation type, TypeAnalysisResult result) {
		var may = Query.of(type.name() + "#may", (builder, p1) -> {
			if (result.isAbstractType()) {
				for (var subtype : result.getDirectSubtypes()) {
					builder.clause(PartialLiterals.may(subtype.call(p1)));
				}
			} else {
				builder.clause(new MayTypeView(typeSymbol, type).call(p1));
			}
		});

		var must = Query.of(type.name() + "#must", (builder, p1) -> builder.clause(
				new MustTypeView(typeSymbol, type).call(p1)
		));

		var candidate = Query.of(type.name() + "#candidate", (builder, p1) -> {
			if (!result.isAbstractType()) {
				builder.clause(new CandidateTypeView(typeSymbol, type).call(p1));
			}
			for (var subtype : result.getDirectSubtypes()) {
				builder.clause(PartialLiterals.candidateMust(subtype.call(p1)));
			}
		});

		return PartialRelationTranslator.of(type)
				.may(may)
				.must(must)
				.candidate(candidate)
				.refiner(InferredTypeRefiner.of(typeSymbol, result));
	}

	private ModelStoreConfiguration createEliminatedTypeTranslator(
			PartialRelation type, PartialRelation replacement) {
		return new PartialRelationTranslatorProxy(type, replacement, true);
	}
}
