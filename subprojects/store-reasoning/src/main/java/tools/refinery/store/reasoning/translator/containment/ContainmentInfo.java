/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning.translator.containment;

import tools.refinery.store.reasoning.representation.PartialRelation;
import tools.refinery.store.reasoning.translator.TranslationException;
import tools.refinery.store.reasoning.translator.multiplicity.Multiplicity;

public record ContainmentInfo(PartialRelation sourceType, Multiplicity multiplicity,
							  PartialRelation targetType) {
	public ContainmentInfo {
		if (sourceType.arity() != 1) {
			throw new TranslationException(sourceType, "Expected source type %s to be of arity 1, got %d instead"
							.formatted(sourceType, sourceType.arity()));
		}
		if (targetType.arity() != 1) {
			throw new TranslationException(targetType, "Expected target type %s to be of arity 1, got %d instead"
					.formatted(targetType, targetType.arity()));
		}
	}
}
