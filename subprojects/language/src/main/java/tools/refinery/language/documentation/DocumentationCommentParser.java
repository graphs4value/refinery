/*
 * SPDX-FileCopyrightText: 2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.documentation;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.conversion.ValueConverterException;
import org.eclipse.xtext.documentation.IEObjectDocumentationProvider;
import org.eclipse.xtext.util.IResourceScopeCache;
import tools.refinery.language.conversion.IdentifierValueConverter;
import tools.refinery.language.model.problem.*;
import tools.refinery.language.naming.NamingUtil;
import tools.refinery.language.utils.ProblemUtil;

import java.util.*;
import java.util.regex.Pattern;

@Singleton
public class DocumentationCommentParser {
	private static final String PREFIX = "tools.refinery.language.documentation.DocumentationCommentParser.";
	private static final String CACHE_KEY = PREFIX  + "CACHE_KEY";
	public static final String CLASS_PARAMETER_NAME = "node";
	public static final String ENUM_PARAMETER_NAME = "node";
	public static final String REFERENCE_SOURCE_PARAMETER_NAME = "source";
	public static final String REFERENCE_TARGET_PARAMETER_NAME = "target";
	public static final String COLOR_TAG = PREFIX + "COLOR_TAG";
	public static final String DOCUMENTATION = PREFIX + "DOCUMENTATION";

	private static final Pattern SPLIT_PATTERN = Pattern.compile("(?m)^\\s*@(?:param|color)");
	private static final Pattern PARAM_PATTERN = Pattern.compile(
			"^@param\\s+(" + NamingUtil.IDENTIFIER_REGEX_STRING + ")");
	private static final Pattern COLOR_PATTERN = Pattern.compile(
			"^@color\\s+(\\d|#[\\da-fA-F]{6}|#[\\da-fA-F]{3})");

	@Inject
	private IResourceScopeCache resourceScopeCache;

	@Inject
	private IEObjectDocumentationProvider documentationProvider;

	@Inject
	private IdentifierValueConverter identifierValueConverter;

	public Map<String, String> parseDocumentation(EObject eObject) {
		if (eObject == null) {
			return Map.of();
		}
		var resource = eObject.eResource();
		if (resource == null) {
			return doParseDocumentation(eObject);
		}
		var cache = resourceScopeCache.get(CACHE_KEY, resource, HashMap<EObject, Map<String, String>>::new);
		return cache.computeIfAbsent(eObject, this::doParseDocumentation);
	}

	private Map<String, String> doParseDocumentation(EObject eObject) {
		if (eObject instanceof Problem) {
			// There is no way to attach a documentation comment to a top-level module.
			return Map.of();
		}
		var documentation = documentationProvider.getDocumentation(eObject);
		return parseDocumentationText(eObject, Objects.requireNonNullElse(documentation, ""));

	}

	private Map<String, String> parseDocumentationText(EObject eObject, String documentation) {
		var splits = splitDocumentation(documentation);
		var parsedMap = new LinkedHashMap<String, String>();
		var documentationPrefix = splits.removeFirst();
		var defaultParameters = getDefaultParameterDocumentation(eObject);
		var parameters = new LinkedHashMap<>(defaultParameters);
		String color = null;
		while (!splits.isEmpty()) {
			var split = splits.removeFirst();
			var colorMatch = COLOR_PATTERN.matcher(split);
			if (color == null && colorMatch.find()) {
				// Use a {@code _} instead of a {@code #} to signify hex codes, because the type hashes have to be
				// valid CSS class names.
				color = colorMatch.group(1).toLowerCase(Locale.ROOT);
				parsedMap.put(COLOR_TAG, color.replace("#", "_"));
			}
			var paramMatch = PARAM_PATTERN.matcher(split);
			if (paramMatch.find()) {
				var paramNameString = paramMatch.group(1);
				String paramName;
				try {
					paramName = identifierValueConverter.toValue(paramNameString, null);
				} catch (ValueConverterException e) {
					// Ignore malformed identifier and skip the corresponding documentation.
					paramName = null;
				}
				if (paramName != null) {
					var paramDescription = split.substring(paramMatch.end()).trim();
					parameters.put(paramName, paramDescription);
				}
			}
		}
		var transformedDocumentation = formatDocumentation(documentationPrefix, parameters, defaultParameters, color);
		if (!Strings.isNullOrEmpty(transformedDocumentation)) {
			parsedMap.put(DOCUMENTATION, transformedDocumentation);
		}
		return Collections.unmodifiableMap(parsedMap);
	}

	private Deque<String> splitDocumentation(String documentation) {
		var splits = new ArrayDeque<String>();
		int startIndex = -1;
		var matcher = SPLIT_PATTERN.matcher(documentation);
		while (matcher.find(startIndex + 1)) {
			int endIndex = matcher.start();
			splits.addLast(documentation.substring(Math.max(0, startIndex), endIndex).strip());
			startIndex = endIndex;
		}
		splits.addLast(documentation.substring(Math.max(0, startIndex)).strip());
		return splits;
	}

	private Map<String, String> getDefaultParameterDocumentation(EObject eObject) {
		return switch (eObject) {
			case ClassDeclaration classDeclaration -> Map.of(CLASS_PARAMETER_NAME,
					"The instance of the `%s` class.".formatted(formatIdentifier(classDeclaration.getName())));
			case EnumDeclaration enumDeclaration -> Map.of(ENUM_PARAMETER_NAME,
					"The instance of the `%s` enumeration.".formatted(formatIdentifier(enumDeclaration.getName())));
			case ReferenceDeclaration referenceDeclaration -> {
				var formattedName = formatIdentifier(referenceDeclaration.getName());
				var linkType = ProblemUtil.isContainmentReference(referenceDeclaration) ? "containment" : "reference";
				// Use {@link ImmutableMap} to maintain insertion order.
				yield ImmutableMap.of(
						REFERENCE_SOURCE_PARAMETER_NAME,
						"The source of the `%s` %s link.".formatted(formattedName, linkType),
						REFERENCE_TARGET_PARAMETER_NAME,
						"The target of the `%s` %s link.".formatted(formattedName, linkType));
			}
			case ParametricDefinition parametricDefinition -> {
				// We must allow duplicates, because this could be called on an invalid model.
				var map = new LinkedHashMap<String, String>();
				for (var parameter : parametricDefinition.getParameters()) {
					var name = parameter.getName();
					if (name != null) {
						map.put(name, "");
					}
				}
				yield map;
			}
			case null, default -> Map.of();
		};
	}

	private String formatIdentifier(String identifier) {
		if (identifier == null) {
			return "?";
		}
		return identifierValueConverter.toString(identifier).replace("<", "&lt;");
	}

	private String formatDocumentation(String documentationPrefix, Map<String, String> parameters,
									   Map<String, String> defaultParameters, String color) {
		var builder = new StringBuilder();
		if (!Strings.isNullOrEmpty(documentationPrefix)) {
			builder.append(documentationPrefix).append("\n");
		}
		if (!parameters.isEmpty()) {
			builder.append("\n### Parameters\n<dl class=\"refinery-completion-parameters\">\n");
			for (var entry : parameters.entrySet()) {
				var parameterName = entry.getKey();
				var extraClass = "";
				if (!defaultParameters.containsKey(parameterName)) {
					extraClass = " refinery-completion-parameter-invalid";
				}
				builder.append("<dt class=\"refinery-completion-parameter-name")
						.append(extraClass)
						.append("\">\n\n")
						.append(formatIdentifier(parameterName))
						.append("\n\n</dt>\n<dd class=\"refinery-completion-parameter-description")
						.append(extraClass)
						.append("\">\n\n")
						.append(entry.getValue())
						.append("\n\n</dd>\n");
			}
			builder.append("</dl>\n");
		}
		if (color != null) {
			builder.append("\n### Color\n")
					.append(color.replace("#", "&#35;"))
					.append("\n");
		}
		return builder.toString();
	}
}
