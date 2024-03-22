/*
 * SPDX-FileCopyrightText: 2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.documentation;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.documentation.IEObjectDocumentationProvider;

import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

@Singleton
public class DocumentationCommentParser {
	private static final String PREFIX = "tools.refinery.language.documentation.DocumentationCommentParser.";
	public static final String COLOR_TAG = PREFIX + "COLOR_TAG";

	private static final Pattern COLOR_PATTERN = Pattern.compile(
			"(?m)^@color[ \t]+(\\d|#[\\da-fA-F]{6}|#[\\da-fA-F]{3})");

	@Inject
	private IEObjectDocumentationProvider documentationProvider;

	public Map<String, String> parseDocumentation(EObject eObject) {
		var documentation = documentationProvider.getDocumentation(eObject);
		if (documentation == null) {
			return Map.of();
		}
		var colorMatch = COLOR_PATTERN.matcher(documentation);
		if (colorMatch.find()) {
			// Use a {@code _} instead of a {@code #} to signify hex codes, because the type hashes have to be valid
			// CSS class names.
			var color = colorMatch.group(1).toLowerCase(Locale.ROOT).replace("#", "_");
			return Map.of(COLOR_TAG, color);
		}
		return Map.of();
	}
}
