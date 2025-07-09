/*
 * SPDX-FileCopyrightText: 2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.documentation;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

record ParsedDocumentation(@Nullable String documentation, @Nullable String color,
						   @NotNull Map<String, String> parameterDocumentation) {
	public static final ParsedDocumentation EMPTY = new ParsedDocumentation(null);

	public ParsedDocumentation(@Nullable String documentation) {
		this(documentation, null, Map.of());
	}
}
