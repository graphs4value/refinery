/*
 * SPDX-FileCopyrightText: 2021-2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.naming;

import org.eclipse.xtext.naming.QualifiedName;

import java.util.regex.Pattern;

public final class NamingUtil {
	public static final QualifiedName ROOT_NAME = QualifiedName.create("");

	private static final Pattern ID_REGEX = Pattern.compile("[_a-zA-Z]\\w*");

	private NamingUtil() {
		throw new IllegalStateException("This is a static utility class and should not be instantiated directly");
	}

	public static boolean isNullOrEmpty(String name) {
		return name == null || name.isEmpty();
	}

	public static boolean isSingletonVariableName(String name) {
		return name != null && !name.isEmpty() && name.charAt(0) == '_' && isSimpleId(name);
	}

	// This method name only makes sense if it checks for the positive case.
	@SuppressWarnings("BooleanMethodIsAlwaysInverted")
	public static boolean isValidId(String name) {
		return name != null && scanName(name, 0) == name.length();
	}

	public static boolean isSimpleId(String name) {
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

	public static boolean isQuoted(String name) {
		return !name.isEmpty() && name.charAt(0) == '\'';
	}

	// Full state machine kept in a single method.
	@SuppressWarnings("squid:S3776")
	static int scanName(String input, int startIndex) {
		int index = startIndex;
		var state = ScannerState.INITIAL;
		int length = input.length();
		while (index < length) {
			char c = input.charAt(index);
			switch (state) {
			case INITIAL -> {
				if (validIdStart(c)) {
					state = ScannerState.UNQUOTED;
				} else if (c == '\'') {
					state = ScannerState.QUOTED;
				} else {
					return index;
				}
			}
			case UNQUOTED -> {
				if (!validId(c)) {
					return index;
				}
			}
			case QUOTED -> {
				if (c == '\\') {
					state = ScannerState.ESCAPE;
				} else if (c == '\'') {
					return index + 1;
				}
			}
			case ESCAPE -> state = ScannerState.QUOTED;
			default -> throw new IllegalStateException("Unexpected state: " + state);
			}
			index++;
		}
		return state == ScannerState.UNQUOTED ? index : -(index + 1);
	}

	private static boolean validIdStart(char c) {
		return c == '_' || (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z');
	}

	private static boolean validId(char c) {
		return validIdStart(c) || (c >= '0' && c <= '9');
	}

	private enum ScannerState {
		INITIAL,
		UNQUOTED,
		QUOTED,
		ESCAPE
	}
}
