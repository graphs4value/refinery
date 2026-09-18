/*
 * SPDX-FileCopyrightText: 2026 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.generator.cli.commands;

import com.beust.jcommander.Parameter;
import com.google.inject.Inject;
import tools.refinery.generator.ModelSemanticsFactory;
import tools.refinery.generator.cli.RefineryCli;
import tools.refinery.generator.cli.output.CliProblemOutput;
import tools.refinery.generator.cli.output.OutputOptions;
import tools.refinery.generator.cli.utils.CliProblemLoader;

import java.io.IOException;

public abstract class AbstractSemanticsCommand implements OutputFormatCommand {
	private final CliProblemLoader loader;
	private final ModelSemanticsFactory semanticsFactory;
	private final CliProblemOutput serializer;

	private String inputPath;

	@Inject
	public AbstractSemanticsCommand(CliProblemLoader loader, ModelSemanticsFactory semanticsFactory,
                                    CliProblemOutput serializer) {
		this.loader = loader;
		this.semanticsFactory = semanticsFactory;
		this.serializer = serializer;
	}

	protected String getInputPath() {
		return inputPath;
	}

	protected CliProblemOutput getSerializer() {
		return serializer;
	}

	@Parameter(description = "input path", required = true)
	public void setInputPath(String inputPath) {
		this.inputPath = inputPath;
    }

	public abstract OutputOptions getOutputOptions();

	protected boolean isConcretize() {
		return false;
	}

	@Override
	public int run() throws IOException {
		var problem = loader.loadProblem(inputPath);
		try (var semantics = semanticsFactory.concretize(isConcretize()).createSemantics(problem)) {
			serializer.saveModel(getOutputOptions(), semantics);
		}
		return RefineryCli.EXIT_SUCCESS;
	}
}
