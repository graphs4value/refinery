/*
 * SPDX-FileCopyrightText: 2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning.literal;

import tools.refinery.logic.InvalidQueryException;

import java.util.Locale;

public enum ConcretenessSpecification {
	UNSPECIFIED,
	PARTIAL,
	CANDIDATE;

	public ConcretenessSpecification orElse(ConcretenessSpecification concreteness) {
		return this == UNSPECIFIED ? concreteness : this;
	}

	public Concreteness toConcreteness() {
		return switch (this) {
			case UNSPECIFIED -> throw new InvalidQueryException("Unspecified concreteness");
			case PARTIAL -> Concreteness.PARTIAL;
			case CANDIDATE -> Concreteness.CANDIDATE;
		};
	}

	@Override
	public String toString() {
		return name().toLowerCase(Locale.ROOT);
	}

}
