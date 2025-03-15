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
import tools.refinery.generator.cli.utils.CliProblemSerializer;
import tools.refinery.generator.cli.utils.CliUtils;

import java.io.IOException;

@Parameters(commandDescription = "Concretize a partial model")
public class ConcretizeCommand implements Command {
	private final CliProblemLoader loader;
	private final ModelSemanticsFactory semanticsFactory;
	private final CliProblemSerializer serializer;

	private String inputPath;
	private String outputPath = CliUtils.STANDARD_OUTPUT_PATH;

	@Inject
	public ConcretizeCommand(CliProblemLoader loader, ModelSemanticsFactory semanticsFactory,
							 CliProblemSerializer serializer) {
		this.loader = loader;
		this.semanticsFactory = semanticsFactory;
		this.serializer = serializer;
	}

	@Parameter(description = "input path", required = true)
	public void setInputPath(String inputPath) {
		this.inputPath = inputPath;
	}

	@Parameter(names = {"-output", "-o"}, description = "Output path")
	public void setOutputPath(String outputPath) {
		this.outputPath = outputPath;
	}

	@Override
	public int run() throws IOException {
		var problem = loader.loadProblem(inputPath);
		try (var semantics = semanticsFactory.concretize(true).createSemantics(problem)) {
			serializer.saveModel(semantics, outputPath);
		}
		return RefineryCli.EXIT_SUCCESS;
	}
}
