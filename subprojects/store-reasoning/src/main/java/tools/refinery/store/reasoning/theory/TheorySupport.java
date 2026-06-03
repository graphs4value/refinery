/*
 * SPDX-FileCopyrightText: 2026 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning.theory;

public enum TheorySupport {
	ENABLED_BY_DEFAULT,
	EXPLICIT_ONLY,
	UNSUPPORTED;

	public boolean isEnabledByDefault() {
		return this == ENABLED_BY_DEFAULT;
	}

	public boolean isSupported() {
		return this != UNSUPPORTED;
	}

	public TheorySupport meet(TheorySupport other) {
		if (this == UNSUPPORTED || other == UNSUPPORTED) {
			return UNSUPPORTED;
		}
		if (this == EXPLICIT_ONLY || other == EXPLICIT_ONLY) {
			return EXPLICIT_ONLY;
		}
		return ENABLED_BY_DEFAULT;
	}
}
