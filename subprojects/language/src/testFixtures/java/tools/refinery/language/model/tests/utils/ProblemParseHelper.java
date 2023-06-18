/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.model.tests.utils;

import org.eclipse.xtext.testing.util.ParseHelper;

import com.google.inject.Inject;

import tools.refinery.language.model.problem.Problem;

public class ProblemParseHelper {
	@Inject
	private ParseHelper<Problem> parseHelper;
	
	public WrappedProblem parse(String text) {
		Problem problem;
		try {
			problem = parseHelper.parse(text);
		} catch (Exception e) {
			throw new RuntimeException("Unexpected exception while parsing Problem", e);
		}
		return new WrappedProblem(problem);
	}
}
