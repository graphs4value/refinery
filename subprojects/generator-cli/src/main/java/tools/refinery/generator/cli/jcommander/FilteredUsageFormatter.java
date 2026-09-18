/*
 * Copyright (C) 2010 the original author or authors.
 * See the notice.md file distributed with this work for additional
 * information regarding copyright ownership.
 * Copyright (C) 2026 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 * Portions of this file have been adapted from
 * https://github.com/cbeust/jcommander/blob/a1d85057b6f6d4cca2fef965c5367bde9da45860/src/main/java/com/beust/jcommander/DefaultUsageFormatter.java
 * The {@code appendAllParameterDetails} method was modified to allow dynamically hiding options and enum values and
 * to display possible enum and boolean values as placeholders.
 */
package tools.refinery.generator.cli.jcommander;

import com.beust.jcommander.*;

import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class FilteredUsageFormatter extends DefaultUsageFormatter {
	public FilteredUsageFormatter(JCommander commander) {
		super(commander);
	}

	/**
	 * Appends the details of all parameters in the given order to the argument string builder, indenting every
	 * line with indentCount-many indent.
	 * <p>
	 *     Portions of this method have been adapted from
	 *     <a href="https://github.com/cbeust/jcommander/blob/a1d85057b6f6d4cca2fef965c5367bde9da45860/src/main/java/com/beust/jcommander/DefaultUsageFormatter.java">
	 *     https://github.com/cbeust/jcommander/blob/a1d85057b6f6d4cca2fef965c5367bde9da45860/src/main/java/com/beust/jcommander/DefaultUsageFormatter.java
	 *     </a>. This method was modified to allow dynamically hiding options and enum values and to display
	 *     potential enum and boolean values as placeholders.
	 * </p>
	 *
	 * @param out the builder to append to
	 * @param indentCount the amount of indentation to apply
	 * @param indent the indentation
	 * @param sortedParameters the parameters to append to the builder
	 */
	@Override
	public void appendAllParametersDetails(StringBuilder out, int indentCount, String indent,
	                                       List<ParameterDescription> sortedParameters) {
		// {@link #usage(StringBuilder, String)} already filters out parameters marked as hidden statically,
		// but we add a new dynamic check here.
		sortedParameters = sortedParameters.stream()
				.filter(parameterDescription -> !isHidden(parameterDescription))
				.toList();

		if (!sortedParameters.isEmpty()) {
			out.append('\n');
			out.append(indent).append("  Options:\n");
		}

		for (ParameterDescription pd : sortedParameters) {
			WrappedParameter parameter = pd.getParameter();
			String description = pd.getDescription();
			boolean hasDescription = !description.isEmpty();
			Class<?> type = pd.getParameterized().getType();
			// {@code arity} may be -1 if it's omitted from the annotation, but only a value of 1 means a parameter
			// with an explicit Boolean value instead of a flag.
			boolean isFlag = Boolean.TYPE.equals(type) && parameter.arity() < 1;

			var placeholder = parameter.placeholder();
			String possibleValuesString = null;
			if (type.isEnum()) {
				// Allow overriding the range of displayed values.
				// We've just checked that {@code type} is an enum type.
				@SuppressWarnings({"unchecked", "rawtypes"})
				Collection<?> possibleValues = this.getPossibleValues((Class<? extends Enum>) type);
				if (placeholder.isBlank()) {
					placeholder = possibleValues.stream()
							.map(Objects::toString)
							.collect(Collectors.joining("|"));
				} else {
					possibleValuesString = "Possible Values: " + possibleValues;
				}
			} else if (Boolean.TYPE.equals(type) && !isFlag) {
				if (placeholder.isBlank()) {
					placeholder = "true|false";
				} else {
					possibleValuesString = "Possible Values: [true, false]";
				}
			}

			// First line, command name
			out.append(indent)
					.append("  ")
					.append(parameter.required() ? "* " : "  ")
					.append(pd.getNames())
					.append(placeholder.isBlank() ? "" : " " + placeholder)
					.append('\n');

			if (hasDescription) {
				wrapDescription(out, indentCount, s(indentCount) + description);
			}

			String category = pd.getCategory();
			if (!category.isEmpty()) {
				String categoryType = "Category: " + category;

				if (hasDescription) {
					out.append(newLineAndIndent(indentCount));
				} else {
					out.append(s(indentCount));
				}
				out.append(categoryType);
			}

			Object def = pd.getDefaultValueDescription();

			if (pd.isDynamicParameter()) {
				String syntax = "Syntax: " + parameter.names()[0] + "key" + parameter.getAssignment() + "value";

				if (hasDescription) {
					out.append(newLineAndIndent(indentCount));
				} else {
					out.append(s(indentCount));
				}
				out.append(syntax);
			}

			// Hide {@code Default: false} for flag options.
			if (def != null && !pd.isHelp() && !isFlag) {
				String displayedDef = Strings.isStringEmpty(def.toString()) ? "<empty string>" : def.toString();
				String defaultText = "Default: " + (parameter.password() ? "********" : displayedDef);

				if (hasDescription) {
					out.append(newLineAndIndent(indentCount));
				} else {
					out.append(s(indentCount));
				}
				out.append(defaultText);
			}

			// Prevent duplicate values list, since it is set as 'Options: [values]' if the description
			// of an enum field is empty in ParameterDescription#init(..)
			if (possibleValuesString != null && !description.contains("Options: ")) {
				if (hasDescription) {
					out.append(newLineAndIndent(indentCount));
				} else {
					out.append(s(indentCount));
				}
				out.append(possibleValuesString);
			}
			out.append('\n');
		}
	}

	protected boolean isHidden(ParameterDescription parameterDescription) {
		return false;
	}

	protected <E extends Enum<E>> EnumSet<E> getPossibleValues(Class<E> type) {
		return EnumSet.allOf(type);
	}

	/**
	 * Returns new line followed by indent-many spaces.
	 * <p>
	 *     This method has been copied from
	 *     <a href="https://github.com/cbeust/jcommander/blob/a1d85057b6f6d4cca2fef965c5367bde9da45860/src/main/java/com/beust/jcommander/DefaultUsageFormatter.java">
	 *     https://github.com/cbeust/jcommander/blob/a1d85057b6f6d4cca2fef965c5367bde9da45860/src/main/java/com/beust/jcommander/DefaultUsageFormatter.java
	 *     </a>. No behavioral changes were made.
	 * </p>
	 *
	 * @return new line followed by indent-many spaces
	 */
	private static String newLineAndIndent(int indent) {
		return "\n" + s(indent);
	}
}
