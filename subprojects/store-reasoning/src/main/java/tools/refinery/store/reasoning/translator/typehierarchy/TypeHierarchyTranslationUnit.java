/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning.translator.typehierarchy;

import tools.refinery.store.model.Model;
import tools.refinery.store.reasoning.representation.PartialRelation;
import tools.refinery.store.reasoning.translator.TranslatedRelation;
import tools.refinery.store.reasoning.translator.TranslationUnit;
import tools.refinery.store.representation.Symbol;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public class TypeHierarchyTranslationUnit extends TranslationUnit {
	static final Symbol<InferredType> INFERRED_TYPE_SYMBOL = new Symbol<>("inferredType", 1,
			InferredType.class, InferredType.UNTYPED);

	private final TypeAnalyzer typeAnalyzer;

	public TypeHierarchyTranslationUnit(Map<PartialRelation, TypeInfo> typeInfoMap) {
		typeAnalyzer = new TypeAnalyzer(typeInfoMap);
	}

	@Override
	public Collection<TranslatedRelation> getTranslatedRelations() {
		return List.of();
	}

	@Override
	public void initializeModel(Model model, int nodeCount) {

	}
}
