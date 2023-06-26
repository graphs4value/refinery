/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning.literal;

import java.util.Locale;

public enum Concreteness {
	PARTIAL,
	CANDIDATE;

	@Override
	public String toString() {
		return name().toLowerCase(Locale.ROOT);
	}
}
