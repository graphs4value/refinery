/*
 * SPDX-FileCopyrightText: 2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning.literal;

import java.util.Locale;

public enum ModalitySpecification {
	UNSPECIFIED,
	MUST,
	MAY;

	public ModalitySpecification negate() {
		return switch (this) {
			case UNSPECIFIED -> UNSPECIFIED;
			case MUST -> MAY;
			case MAY -> MUST;
		};
	}

	public ModalitySpecification orElse(ModalitySpecification modality) {
		return this == UNSPECIFIED ? modality : this;
	}

	public Modality toModality() {
		return switch (this) {
			case UNSPECIFIED -> throw new IllegalStateException("Unspecified modality");
			case MUST -> Modality.MUST;
			case MAY -> Modality.MAY;
		};
	}

	@Override
	public String toString() {
		return name().toLowerCase(Locale.ROOT);
	}
}
