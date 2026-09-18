/*
 * SPDX-FileCopyrightText: 2026 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.generator.cli.output;

import java.util.Locale;

public enum OutputTheme {
	LIGHT,
	DARK,
	AUTO;

	public boolean isStatic() {
		return this != AUTO;
	}

	public boolean supportsEmbedFonts() {
		return isStatic();
	}

	public boolean supportsSolidBackground() {
		return isStatic();
	}

	public OutputTheme asStaticTheme() {
		return isStatic() ? this : LIGHT;
	}

	@Override
	public String toString() {
		return name().toLowerCase(Locale.ROOT);
	}
}
