/*
 * SPDX-FileCopyrightText: 2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.generator.tests;

import tools.refinery.generator.ModelSemanticsFactory;

import java.util.List;

public record SemanticsTest(List<SemanticsTestCase> testCases) {
	public void execute(ModelSemanticsFactory semanticsFactory) {
		for (var testCase : testCases) {
			testCase.execute(semanticsFactory);
		}
	}
}
