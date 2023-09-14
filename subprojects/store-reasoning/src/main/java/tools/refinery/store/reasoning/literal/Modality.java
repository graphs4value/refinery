/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning.literal;

import tools.refinery.store.query.literal.CallPolarity;

import java.util.Locale;

public enum Modality {
	MUST,
	MAY;

	public Modality negate() {
		return switch(this) {
			case MUST -> MAY;
			case MAY -> MUST;
		};
	}

	public Modality commute(CallPolarity polarity) {
		if (polarity.isPositive()) {
			return this;
		}
		return this.negate();
	}

	@Override
	public String toString() {
		return name().toLowerCase(Locale.ROOT);
	}
}
