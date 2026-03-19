/*
 * SPDX-FileCopyrightText: 2026 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.gradle.utils;

import java.util.List;

public class JvmArgsUtils {
	public static final List<String> JVM_ARGS = List.of(
			"--enable-native-access=ALL-UNNAMED",
			"--sun-misc-unsafe-memory-access=allow"
	);

	private JvmArgsUtils() {
		throw new IllegalStateException("This is a static utility class and should not be instantiated directly");
	}
}
