/*
 * SPDX-FileCopyrightText: 2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.generator.cli.utils;

import com.google.inject.Inject;
import tools.refinery.generator.ProblemLoader;
import tools.refinery.language.model.problem.Problem;

import java.io.IOException;
import java.util.List;

public class CliProblemLoader {
	private final ProblemLoader loader;

	@Inject
	public CliProblemLoader(ProblemLoader loader) {
		this.loader = loader;
		loader.extraPath(System.getProperty("user.dir"));
	}

	public Problem loadProblem(String inputPath) throws IOException {
		return CliUtils.isStandardStream(inputPath) ? loader.loadStream(System.in) : loader.loadFile(inputPath);
	}

	public Problem loadProblem(String inputPath, List<String> extraScopes,
							   List<String> overrideScopes) throws IOException {
		var problem = loadProblem(inputPath);
		return loader.loadScopeConstraints(problem, extraScopes, overrideScopes);
	}
}
