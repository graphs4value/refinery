/*
 * SPDX-FileCopyrightText: 2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.documentation;

import java.util.Map;

public record ParsedDocumentation(Map<String, String> userData, Map<String, String> parameterDocumentation) {
	public static final ParsedDocumentation EMPTY = new ParsedDocumentation(Map.of(), Map.of());
}
