/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.query.tests;

import org.hamcrest.Matcher;
import tools.refinery.store.query.dnf.Dnf;

public final class QueryMatchers {
	private QueryMatchers() {
		throw new IllegalStateException("This is a static utility class and should not be instantiated directly");
	}

	public static Matcher<Dnf> structurallyEqualTo(Dnf expected) {
		return new StructurallyEqualTo(expected);
	}
}
