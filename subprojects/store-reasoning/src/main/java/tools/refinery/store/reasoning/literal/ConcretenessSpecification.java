/*
 * SPDX-FileCopyrightText: 2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning.literal;

import java.util.Locale;

//Concretenesshez képest lehet unspecified is.
public enum ConcretenessSpecification {
	UNSPECIFIED,
	PARTIAL,
	CANDIDATE;

	//Ha ez unspecified akkor a paraméterként kapottat adja vissza, különben önmagát.
	public ConcretenessSpecification orElse(ConcretenessSpecification concreteness) {
		return this == UNSPECIFIED ? concreteness : this;
	}

	//Ennek van fordítottja is, ha unspecifiedat mappelnénk akkor error.
	public Concreteness toConcreteness() {
		return switch (this) {
			case UNSPECIFIED -> throw new IllegalStateException("Unspecified concreteness");
			case PARTIAL -> Concreteness.PARTIAL;
			case CANDIDATE -> Concreteness.CANDIDATE;
		};
	}

	@Override
	public String toString() {
		return name().toLowerCase(Locale.ROOT);
	}

}
