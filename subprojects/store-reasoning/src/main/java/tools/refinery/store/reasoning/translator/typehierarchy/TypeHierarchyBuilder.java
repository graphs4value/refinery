/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning.translator.typehierarchy;

import tools.refinery.store.reasoning.representation.PartialRelation;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class TypeHierarchyBuilder {
	private final Map<PartialRelation, TypeInfo> typeInfoMap = new LinkedHashMap<>();

	public TypeHierarchyBuilder type(PartialRelation partialRelation, TypeInfo typeInfo) {
		if (partialRelation.arity() != 1) {
			throw new IllegalArgumentException("Only types of arity 1 are supported, hot %d instead"
					.formatted(partialRelation.arity()));
		}
		var putResult = typeInfoMap.put(partialRelation, typeInfo);
		if (putResult != null && !putResult.equals(typeInfo)) {
			throw new IllegalArgumentException("Duplicate type info for partial relation: " + partialRelation);
		}
		return this;
	}

	public TypeHierarchyBuilder type(PartialRelation partialRelation, boolean abstractType,
									 PartialRelation... supertypes) {
		return type(partialRelation, abstractType, List.of(supertypes));
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
