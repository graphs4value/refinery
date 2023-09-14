/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning.translator.metamodel;

import tools.refinery.store.reasoning.representation.PartialRelation;
import tools.refinery.store.reasoning.translator.TranslationException;
import tools.refinery.store.reasoning.translator.containment.ContainmentHierarchyTranslator;
import tools.refinery.store.reasoning.translator.typehierarchy.TypeHierarchyBuilder;

import java.util.Collection;

public class ContainedTypeHierarchyBuilder extends TypeHierarchyBuilder {
	ContainedTypeHierarchyBuilder() {
	}

	boolean isInvalidType(PartialRelation type) {
		return !typeInfoMap.containsKey(type);
	}

	void setContainedTypes(Collection<PartialRelation> containedTypes) {
		for (var containedType : containedTypes) {
			var currentInfo = typeInfoMap.get(containedType);
			if (currentInfo == null) {
				throw new TranslationException(containedType, "Invalid contained type: " + containedType);
			}
			var newInfo = currentInfo.addSupertype(ContainmentHierarchyTranslator.CONTAINED_SYMBOL);
			typeInfoMap.put(containedType, newInfo);
		}
	}
}
