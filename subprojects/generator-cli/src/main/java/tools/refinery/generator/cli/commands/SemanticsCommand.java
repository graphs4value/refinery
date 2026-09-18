/*
 * SPDX-FileCopyrightText: 2026 The Refinery Authors <https://refinery.tools/>
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

@Parameters(commandDescription = "Evaluate a partial model without concretization")
public class SemanticsCommand extends AbstractSemanticsCommand {
	@ParametersDelegate
	private final OutputOptions.Json outputOptions = new OutputOptions.Json();

	@Inject
	public SemanticsCommand(CliProblemLoader loader, ModelSemanticsFactory semanticsFactory,
	                        CliProblemOutput serializer) {
		super(loader, semanticsFactory, serializer);
	}

	public OutputOptions.Json getOutputOptions() {
		return outputOptions;
	}
}
