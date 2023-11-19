/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.model.tests.utils;

import com.google.inject.Inject;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.xtext.testing.util.ParseHelper;
import org.eclipse.xtext.validation.IResourceValidator;
import tools.refinery.language.model.problem.Problem;

public class ProblemParseHelper {
	@Inject
	private IResourceValidator resourceValidator;
	@Inject
	private ParseHelper<Problem> parseHelper;

	public WrappedProblem parse(String text) {
		Problem problem;
		try {
			problem = parseHelper.parse(text);
		} catch (Exception e) {
			throw new AssertionError("Unexpected exception while parsing Problem", e);
		}
		EcoreUtil.resolveAll(problem);
		return new WrappedProblem(problem);
	}
}
