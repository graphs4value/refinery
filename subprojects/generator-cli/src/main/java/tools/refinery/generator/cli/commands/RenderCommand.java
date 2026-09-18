/*
 * SPDX-FileCopyrightText: 2026 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.generator.cli.commands;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.beust.jcommander.ParametersDelegate;
import com.google.inject.Inject;
import tools.refinery.generator.ModelSemanticsFactory;
import tools.refinery.generator.cli.output.CliProblemOutput;
import tools.refinery.generator.cli.output.InputFormat;
import tools.refinery.generator.cli.output.OutputFormat;
import tools.refinery.generator.cli.output.OutputOptions;
import tools.refinery.generator.cli.utils.CliProblemLoader;
import tools.refinery.generator.cli.utils.CliUtils;

import java.io.FileInputStream;
import java.io.IOException;

@Parameters(commandDescription = "Render a partial model as an image")
public class RenderCommand extends AbstractSemanticsCommand {
	private InputFormat inputFormat;

	@ParametersDelegate
	private final OutputOptions.Png outputOptions = new OutputOptions.Png();

	@Inject
	public RenderCommand(CliProblemLoader loader, ModelSemanticsFactory semanticsFactory,
	                     CliProblemOutput serializer) {
		super(loader, semanticsFactory, serializer);
	}

	@Parameter(names = {"-input-format", "-i"}, description = "Partial model input format. If not specified, the " +
			"format is auto-detected from the input file name.")
	public void setInputFormat(InputFormat inputFormat) {
		this.inputFormat = inputFormat;
	}

	public OutputOptions.Png getOutputOptions() {
		return outputOptions;
	}

	@Override
	public int run() throws IOException {
		var inputPath = getInputPath();
		if (inputFormat == InputFormat.REFINERY ||
				(inputFormat == null && OutputFormat.REFINERY.matchesFilePath(inputPath))) {
			return super.run();
		}
		byte[] jsonBytes;
		if (CliUtils.isStandardStream(inputPath)) {
			jsonBytes = System.in.readAllBytes();
		} else {
			try (var inputStream = new FileInputStream(inputPath)) {
				jsonBytes = inputStream.readAllBytes();
			}
		}
		getSerializer().saveGraphical(getOutputOptions(), jsonBytes);
		return 0;
	}
}
