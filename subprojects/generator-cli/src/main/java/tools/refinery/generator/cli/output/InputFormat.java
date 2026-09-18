/*
 * SPDX-FileCopyrightText: 2026 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.generator.cli.output;

import java.util.Locale;

public enum InputFormat {
	REFINERY,
	JSON;

	@Override
	public String toString() {
		return name().toLowerCase(Locale.ROOT);
	}
}
