/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.query.literal;

import java.util.Locale;

public enum Connectivity {
	WEAK,
	STRONG;

	@Override
	public String toString() {
		return name().toLowerCase(Locale.ROOT);
	}
}
