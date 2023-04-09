/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.gradle.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;

public final class SonarPropertiesUtils {
	private SonarPropertiesUtils() {
		throw new IllegalStateException("This is a static utility class and should not be instantiated directly");
	}

	/**
	 * Adds the entries to a Sonar property of list type.
	 * <p>
	 * According to the Sonar Gradle documentation for {@link org.sonarqube.gradle.SonarProperties}, property values
	 * are converted to Strings as follows:
	 * <ul>
	 * <li>{@code Iterable}s are recursively converted and joined into a comma-separated String.</li>
	 * <li>All other values are converted to Strings by calling their {@code toString()} method.</li>
	 * </ul>
	 * Therefore, we use {@link ArrayList} to retain lists entries, which will be recursively converted later.
	 *
	 * @param properties   The sonar properties map returned by
	 *                     {@link org.sonarqube.gradle.SonarProperties#getProperties()}.
	 * @param propertyName The name of the property to append to.
	 * @param entries      The entries to append.
	 */
	public static void addToList(Map<String, Object> properties, String propertyName, String... entries) {
		ArrayList<Object> newValue;
		var currentValue = properties.get(propertyName);
		if (currentValue instanceof ArrayList<?> currentList) {
			@SuppressWarnings("unchecked")
			var objectList = (ArrayList<Object>) currentList;
			newValue = objectList;
		} else if (currentValue == null) {
			newValue = new ArrayList<>(entries.length);
		} else {
			newValue = new ArrayList<>(entries.length + 1);
			newValue.add(currentValue);
		}
		Collections.addAll(newValue, entries);
		properties.put(propertyName, newValue);
	}
}
