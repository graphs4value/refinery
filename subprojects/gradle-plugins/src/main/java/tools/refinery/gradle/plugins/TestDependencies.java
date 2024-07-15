/*
 * SPDX-FileCopyrightText: 2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.gradle.plugins;

import java.util.Locale;

public enum TestDependencies {
	NONE(false, false),
	MINIMAL(true, false),
	FULL(true, true);

	private final boolean addJUnit5;

	private final boolean addOtherDependencies;

	TestDependencies(boolean addJUnit5, boolean addOtherDependencies) {
		this.addJUnit5 = addJUnit5;
		this.addOtherDependencies = addOtherDependencies;
	}

	public boolean isAddJUnit5() {
		return addJUnit5;
	}

	public boolean isAddOtherDependencies() {
		return addOtherDependencies;
	}

	public static TestDependencies valueOfIgnoreCase(String value) {
		return valueOf(value.toUpperCase(Locale.ROOT));
	}
}
