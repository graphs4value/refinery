/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.model.tests.utils;

import java.util.List;
import java.util.stream.Stream;

import tools.refinery.language.model.problem.NamedElement;

class ProblemNavigationUtil {
	private ProblemNavigationUtil() {
		throw new IllegalStateException("This is a static utility class and should not be instantiated directly");
	}

	public static <T extends NamedElement> T named(Stream<? extends T> stream, String name) {
		return stream.filter(statement -> name.equals(statement.getName())).findAny().get();
	}

	public static <T extends NamedElement> T named(List<? extends T> list, String name) {
		return named(list.stream(), name);
	}
}
