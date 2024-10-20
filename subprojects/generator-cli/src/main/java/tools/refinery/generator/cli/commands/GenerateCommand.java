/*
 * SPDX-FileCopyrightText: 2023-2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.generator.cli.commands;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.google.inject.Inject;
import tools.refinery.generator.ModelGeneratorFactory;
import tools.refinery.generator.cli.RefineryCli;
import tools.refinery.generator.cli.utils.CliProblemLoader;
import tools.refinery.generator.cli.utils.CliProblemSerializer;
import tools.refinery.generator.cli.utils.CliUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Parameters(commandDescription = "Generate a model from a partial model")
public class GenerateCommand implements Command {
	private final CliProblemLoader loader;
	private final ModelGeneratorFactory generatorFactory;
	private final CliProblemSerializer serializer;

	private String inputPath;
	private String outputPath = CliUtils.STANDARD_OUTPUT_PATH;
	private List<String> scopes = new ArrayList<>();
	private List<String> overrideScopes = new ArrayList<>();
	private long randomSeed = 1;
	private int count = 1;

	@Inject
	public GenerateCommand(CliProblemLoader loader, ModelGeneratorFactory generatorFactory,
						   CliProblemSerializer serializer) {
		this.loader = loader;
		this.generatorFactory = generatorFactory;
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

	@Parameter(names = {"-scope", "-s"}, description = "Extra scope constraints")
	public void setScopes(List<String> scopes) {
		this.scopes = scopes;
	}

	@Parameter(names = {"-scope-override", "-S"}, description = "Override scope constraints")
	public void setOverrideScopes(List<String> overrideScopes) {
		this.overrideScopes = overrideScopes;
	}

	@Parameter(names = {"-random-seed", "-r"}, description = "Random seed")
	public void setRandomSeed(long randomSeed) {
		this.randomSeed = randomSeed;
	}

	@Parameter(names = {"-solution-number", "-n"}, description = "Maximum number of solutions")
	public void setCount(int count) {
		if (count <= 0) {
			throw new IllegalArgumentException("Count must be positive");
		}
		this.count = count;
	}

	@Override
	public int run() throws IOException {
		if (count > 1 && CliUtils.isStandardStream(outputPath)) {
			throw new IllegalArgumentException("Must provide output path if count is larger than 1");
		}
		var problem = loader.loadProblem(inputPath, scopes, overrideScopes);
		generatorFactory.partialInterpretationBasedNeighborhoods(count >= 2);
		var generator = generatorFactory.createGenerator(problem);
		generator.setRandomSeed(randomSeed);
		generator.setMaxNumberOfSolutions(count);
		generator.generate();
		if (count == 1) {
			serializer.saveModel(generator, outputPath);
		} else {
			int solutionCount = generator.getSolutionCount();
			for (int i = 0; i < solutionCount; i++) {
				generator.loadSolution(i);
				var pathWithIndex = CliUtils.getFileNameWithIndex(outputPath, i + 1);
				serializer.saveModel(generator, pathWithIndex, false);
			}
		}
		return RefineryCli.EXIT_SUCCESS;
	}
}

