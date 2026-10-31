/*
 * SPDX-FileCopyrightText: 2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.generator.cli.commands;

import com.beust.jcommander.Parameters;
import com.beust.jcommander.ParametersDelegate;
import com.google.inject.Inject;
import tools.refinery.generator.ModelSemanticsFactory;
import tools.refinery.generator.cli.output.CliProblemOutput;
import tools.refinery.generator.cli.output.OutputOptions;
import tools.refinery.generator.cli.utils.CliProblemLoader;

@Parameters(commandDescription = "Concretize a partial model")
public class ConcretizeCommand extends AbstractSemanticsCommand {
	@ParametersDelegate
	private final OutputOptions.Refinery outputOptions = new OutputOptions.Refinery();

	@Inject
	public ConcretizeCommand(CliProblemLoader loader, ModelSemanticsFactory semanticsFactory,
							 CliProblemOutput serializer) {
		super(loader, semanticsFactory, serializer);
	}

	@Override
	public OutputOptions.Refinery getOutputOptions() {
		return outputOptions;
	}

	@Override
	protected boolean isConcretize() {
		return true;
	}
}
