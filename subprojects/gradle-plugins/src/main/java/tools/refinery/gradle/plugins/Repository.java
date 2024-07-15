/*
 * SPDX-FileCopyrightText: 2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.gradle.plugins;

import tools.refinery.gradle.plugins.internal.Versions;

import java.util.Locale;

public enum Repository {
	LOCAL,
	SNAPSHOT,
	CENTRAL;

	public static Repository valueOfIgnoreCase(String value) {
		return valueOf(value.toUpperCase(Locale.ROOT));
	}

	// The default value depends on source files generated at build time.
	@SuppressWarnings("ConstantValue")
	public static Repository getDefault() {
		if (Versions.USE_MAVEN_LOCAL) {
			return LOCAL;
		}
		if (Versions.REFINERY_VERSION.endsWith("-SNAPSHOT")) {
			return SNAPSHOT;
		}
		return CENTRAL;
	}
}
