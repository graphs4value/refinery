/*
 * SPDX-FileCopyrightText: 2026 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.generator.cli.output;

import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public enum OutputFormat {
	REFINERY(List.of("refinery", "problem"), false, true),
	JSON("json", false, true),
	SVG("svg", true, true),
	PDF("pdf", true, false),
	PNG("png", true, false);

	private final Pattern extensionPattern;
	private final boolean isGraphical;
	private final boolean allowConsoleOutput;

	OutputFormat(List<String> extensions, boolean isGraphical, boolean allowConsoleOutput) {
		extensionPattern = Pattern.compile("\\.(?:%s)$".formatted(extensions.stream()
				.map(Pattern::quote)
				.collect(Collectors.joining("|"))), Pattern.CASE_INSENSITIVE);
		this.isGraphical = isGraphical;
		this.allowConsoleOutput = allowConsoleOutput;
	}

	OutputFormat(String extension, boolean isGraphical, boolean allowConsoleOutput) {
		this(List.of(extension), isGraphical, allowConsoleOutput);
	}

	public boolean matchesFilePath(String filePath) {
		return extensionPattern.matcher(filePath).find();
	}

	public boolean isGraphical() {
		return isGraphical;
	}

	public boolean isConsoleOutputAllowed() {
		return allowConsoleOutput;
	}

	@Override
	public String toString() {
		return name().toLowerCase(Locale.ROOT);
	}
}
