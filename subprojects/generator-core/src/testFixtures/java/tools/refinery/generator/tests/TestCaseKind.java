/*
 * SPDX-FileCopyrightText: 2026 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.generator.tests;

public enum TestCaseKind {
	NO_ERRORS,
	ALLOW_ERRORS,
	PROPAGATION_FAILURE,
	CONCRETIZATION_FAILURE;

	public boolean noErrors() {
		return this == NO_ERRORS;
	}

	public boolean requiresExpectations() {
		return this == ALLOW_ERRORS;
	}
}
