/*
 * SPDX-FileCopyrightText: 2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.generator.cli.utils;

import java.util.regex.Pattern;

public final class CliUtils {
	public static final String STANDARD_OUTPUT_PATH = "-";

	private static final Pattern EXTENSION_REGEX = Pattern.compile("(.+)\\.([^./\\\\]+)");

	private CliUtils() {
		throw new IllegalStateException("This is a static utility class and should not be instantiated directly");
	}

	public static boolean isStandardStream(String path) {
		return path == null || STANDARD_OUTPUT_PATH.equals(path);
	}

	public static String getFileNameWithIndex(String simpleName, int index) {
		var match = EXTENSION_REGEX.matcher(simpleName);
		if (match.matches()) {
			return "%s_%03d.%s".formatted(match.group(1), index, match.group(2));
		}
		return "%s_%03d".formatted(simpleName, index);
	}
}
