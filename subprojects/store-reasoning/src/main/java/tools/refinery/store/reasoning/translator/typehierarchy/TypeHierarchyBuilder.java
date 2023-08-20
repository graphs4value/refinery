/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning.translator.typehierarchy;

import tools.refinery.store.reasoning.representation.PartialRelation;
import tools.refinery.store.reasoning.translator.TranslationException;

import java.util.*;

@SuppressWarnings("UnusedReturnValue")
public class TypeHierarchyBuilder {
	protected final Map<PartialRelation, TypeInfo> typeInfoMap = new LinkedHashMap<>();

	protected TypeHierarchyBuilder() {
	}

	public TypeHierarchyBuilder type(PartialRelation partialRelation, TypeInfo typeInfo) {
		if (partialRelation.arity() != 1) {
			throw new TranslationException(partialRelation,
					"Only types of arity 1 are supported, got %s with %d instead"
							.formatted(partialRelation, partialRelation.arity()));
		}
		var putResult = typeInfoMap.put(partialRelation, typeInfo);
		if (putResult != null && !putResult.equals(typeInfo)) {
			throw new TranslationException(partialRelation,
					"Duplicate type info for partial relation: " + partialRelation);
		}
		return this;
	}

	public TypeHierarchyBuilder type(PartialRelation partialRelation, boolean abstractType,
									 PartialRelation... supertypes) {
		return type(partialRelation, abstractType, Set.of(supertypes));
	}

	public TypeHierarchyBuilder type(PartialRelation partialRelation, boolean abstractType,
									 Collection<PartialRelation> supertypes) {
		return type(partialRelation, new TypeInfo(supertypes, abstractType));
	}

	public TypeHierarchyBuilder type(PartialRelation partialRelation, PartialRelation... supertypes) {
		return type(partialRelation, List.of(supertypes));
	}

	public TypeHierarchyBuilder type(PartialRelation partialRelation, Collection<PartialRelation> supertypes) {
		return type(partialRelation, false, supertypes);
	}

	public TypeHierarchyBuilder types(Collection<Map.Entry<PartialRelation, TypeInfo>> entries) {
		for (var entry : entries) {
			type(entry.getKey(), entry.getValue());
		}
		return this;
	}

	public TypeHierarchyBuilder types(Map<PartialRelation, TypeInfo> map) {
		return types(map.entrySet());
	}

	public TypeHierarchy build() {
		return new TypeHierarchy(typeInfoMap);
	}
}
