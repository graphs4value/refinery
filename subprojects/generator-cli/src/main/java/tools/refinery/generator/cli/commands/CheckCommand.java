/*
 * SPDX-FileCopyrightText: 2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.generator.cli.commands;


import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.google.inject.Inject;
import tools.refinery.generator.ModelSemanticsFactory;
import tools.refinery.generator.cli.RefineryCli;
import tools.refinery.generator.cli.utils.CliProblemLoader;

import java.io.IOException;

@Parameters(commandDescription = "Check a partial model consistency")
public class CheckCommand implements Command {
	private final CliProblemLoader loader;
	private final ModelSemanticsFactory semanticsFactory;

	private String inputPath;
	private boolean concretize;

	@Inject
	public CheckCommand(CliProblemLoader loader, ModelSemanticsFactory semanticsFactory) {
		this.loader = loader;
		this.semanticsFactory = semanticsFactory;
	}

	@Parameter(description = "input path", required = true)
	public void setInputPath(String inputPath) {
		this.inputPath = inputPath;
	}

	@Parameter(names = {"-concretize", "-k"}, description = "Check the concretization of the partial model")
	public void setConcretize(boolean concretize) {
		this.concretize = concretize;
	}

	@Override
	public int run() throws IOException {
		var problem = loader.loadProblem(inputPath);
		var semantics = semanticsFactory.concretize(concretize)
				.createSemantics(problem);
		var result = semantics.checkConsistency();
		printMessage(result.formatMessage());
		return result.isConsistent() ? RefineryCli.EXIT_SUCCESS : RefineryCli.EXIT_FAILURE;
	}

	// Deliberately print command result to the standard output.
	@SuppressWarnings("squid:S106")
	private void printMessage(String message) {
		System.out.println(message);
	}
}
