/*
 * SPDX-FileCopyrightText: 2023-2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.generator.cli.commands;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.google.inject.Inject;
import org.eclipse.emf.ecore.resource.Resource;
import tools.refinery.generator.ModelGeneratorFactory;
import tools.refinery.generator.ProblemLoader;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@Parameters(commandDescription = "Generate a model from a partial model")
public class GenerateCommand {
	private static final Pattern EXTENSION_REGEX = Pattern.compile("(.+)\\.([^./\\\\]+)");

	@Inject
	private ProblemLoader loader;

	@Inject
	private ModelGeneratorFactory generatorFactory;

	private String inputPath;
	private String outputPath = "-";
	private List<String> scopes = new ArrayList<>();
	private List<String> overrideScopes = new ArrayList<>();
	private long randomSeed = 1;
	private int count = 1;

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

	public void run() throws IOException {
		if (count > 1 && isStandardStream(outputPath)) {
			throw new IllegalArgumentException("Must provide output path if count is larger than 1");
		}
		loader.extraPath(System.getProperty("user.dir"));
		var problem = isStandardStream(inputPath) ? loader.loadStream(System.in) : loader.loadFile(inputPath);
		problem = loader.loadScopeConstraints(problem, scopes, overrideScopes);
		generatorFactory.partialInterpretationBasedNeighborhoods(count >= 2);
		var generator = generatorFactory.createGenerator(problem);
		generator.setRandomSeed(randomSeed);
		generator.setMaxNumberOfSolutions(count);
		generator.generate();
		var saveOptions = Map.of();
		if (count == 1) {
			var solution = generator.serializeSolution();
			var solutionResource = solution.eResource();
			if (isStandardStream(outputPath)) {
				printSolution(solutionResource, saveOptions);
			} else {
				try (var outputStream = new FileOutputStream(outputPath)) {
					solutionResource.save(outputStream, saveOptions);
				}
			}
		} else {
			int solutionCount = generator.getSolutionCount();
			for (int i = 0; i < solutionCount; i++) {
				generator.loadSolution(i);
				var solution = generator.serializeSolution();
				var solutionResource = solution.eResource();
				var pathWithIndex = getFileNameWithIndex(outputPath, i + 1);
				try (var outputStream = new FileOutputStream(pathWithIndex)) {
					solutionResource.save(outputStream, saveOptions);
				}
			}
		}
	}

	private boolean isStandardStream(String path) {
		return path == null || path.equals("-");
	}

	// We deliberately write to the standard output if no output path is specified.
	@SuppressWarnings("squid:S106")
	private void printSolution(Resource solutionResource, Map<?, ?> saveOptions) throws IOException {
		solutionResource.save(System.out, saveOptions);
	}

	private String getFileNameWithIndex(String simpleName, int index) {
		var match = EXTENSION_REGEX.matcher(simpleName);
		if (match.matches()) {
			return "%s_%03d.%s".formatted(match.group(1), index, match.group(2));
		}
		return "%s_%03d".formatted(simpleName, index);
	}
}
