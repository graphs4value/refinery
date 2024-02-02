/*
 * SPDX-FileCopyrightText: 2021-2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.naming;

import org.eclipse.xtext.naming.QualifiedName;

import java.util.regex.Pattern;

public final class NamingUtil {
	private static final String SINGLETON_VARIABLE_PREFIX = "_";
	public static final QualifiedName ROOT_NAME = QualifiedName.create("");

	private static final Pattern ID_REGEX = Pattern.compile("[_a-zA-Z]\\w*");

	private NamingUtil() {
		throw new IllegalStateException("This is a static utility class and should not be instantiated directly");
	}

	public static boolean isNullOrEmpty(String name) {
		return name == null || name.isEmpty();
	}

	public static boolean isSingletonVariableName(String name) {
		return name != null && name.startsWith(SINGLETON_VARIABLE_PREFIX);
	}

	// This method name only makes sense if it checks for the positive case.
	@SuppressWarnings("BooleanMethodIsAlwaysInverted")
	public static boolean isValidId(String name) {
		return name != null && ID_REGEX.matcher(name).matches();
	}

	public static boolean isFullyQualified(QualifiedName name) {
		return name.startsWith(ROOT_NAME);
	}

	public static QualifiedName stripRootPrefix(QualifiedName name) {
		if (name == null) {
			return null;
		}
		return isFullyQualified(name) ? name.skipFirst(ROOT_NAME.getSegmentCount()) : name;
	}

	public static QualifiedName addRootPrefix(QualifiedName name) {
		if (name == null) {
			return null;
		}
		return isFullyQualified(name) ? name : ROOT_NAME.append(name);
	}
}
