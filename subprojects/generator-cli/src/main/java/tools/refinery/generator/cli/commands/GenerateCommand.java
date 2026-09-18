/*
 * SPDX-FileCopyrightText: 2023-2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.generator.cli.commands;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import com.beust.jcommander.Parameters;
import com.beust.jcommander.ParametersDelegate;
import com.google.inject.Inject;
import tools.refinery.generator.ModelGeneratorFactory;
import tools.refinery.generator.cli.RefineryCli;
import tools.refinery.generator.cli.output.CliProblemOutput;
import tools.refinery.generator.cli.output.OutputOptions;
import tools.refinery.generator.cli.utils.CliProblemLoader;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Parameters(commandDescription = "Generate a model from a partial model")
public class GenerateCommand implements OutputFormatCommand {
	private final CliProblemLoader loader;
	private final ModelGeneratorFactory generatorFactory;
	private final CliProblemOutput serializer;

	private String inputPath;
	private List<String> scopes = new ArrayList<>();
	private List<String> overrideScopes = new ArrayList<>();
	private long randomSeed = 1;
	private int count = 1;

	@ParametersDelegate
	private final OutputOptions.Refinery outputOptions = new OutputOptions.Refinery();

	@Inject
	public GenerateCommand(CliProblemLoader loader, ModelGeneratorFactory generatorFactory,
	                       CliProblemOutput serializer) {
		this.loader = loader;
		this.generatorFactory = generatorFactory;
		this.serializer = serializer;
	}

	@Parameter(description = "input path", required = true)
	public void setInputPath(String inputPath) {
		this.inputPath = inputPath;
	}

	@Parameter(names = {"-scope", "-s"}, description = "Extra scope constraints",
			placeholder = "Type1=range,Type2=range,...")
	public void setScopes(List<String> scopes) {
		this.scopes = scopes;
	}

	@Parameter(names = {"-scope-override", "-S"}, description = "Override scope constraints",
		placeholder ="Type1=range,Type2=range,...")
	public void setOverrideScopes(List<String> overrideScopes) {
		this.overrideScopes = overrideScopes;
	}

	@Parameter(names = {"-random-seed", "-r"}, description = "Random seed", placeholder = "SEED")
	public void setRandomSeed(long randomSeed) {
		this.randomSeed = randomSeed;
	}

	@Parameter(names = {"-solution-number", "-n"}, description = "Maximum number of solutions", placeholder = "COUNT")
	public void setCount(int count) {
		if (count <= 0) {
			throw new ParameterException("Count must be positive");
		}
		this.count = count;
	}

	@Override
	public OutputOptions.Refinery getOutputOptions() {
		return outputOptions;
	}

	@Override
	public int run() throws IOException {
		if (count > 1 && outputOptions.isStandardStream()) {
			throw new ParameterException("Must provide output path if count is larger than 1");
		}
		var problem = loader.loadProblem(inputPath, scopes, overrideScopes);
		generatorFactory.partialInterpretationBasedNeighborhoods(count >= 2);
		try (var generator = generatorFactory.createGenerator(problem)) {
			generator.setRandomSeed(randomSeed);
			generator.setMaxNumberOfSolutions(count);
			generator.generate();

			if (count == 1) {
				serializer.saveModel(outputOptions, generator);
			} else {
				int solutionCount = generator.getSolutionCount();
				for (int i = 0; i < solutionCount; i++) {
					generator.loadSolution(i);
					serializer.saveModel(outputOptions, generator, i);
				}
			}
		}
		return RefineryCli.EXIT_SUCCESS;
	}
}
