/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
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
import tools.refinery.language.model.problem.Problem;
import tools.refinery.language.model.problem.Relation;
import tools.refinery.language.model.problem.ScopeDeclaration;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

@Parameters(commandDescription = "Generate a model from a partial model")
public class GenerateCommand {
	@Inject
	private ProblemLoader loader;

	@Inject
	private ModelGeneratorFactory generatorFactory;

	private String problemPath;
	private String outputPath = "-";
	private List<String> scopes = new ArrayList<>();
	private List<String> overrideScopes = new ArrayList<>();
	private long randomSeed = 1;

	@Parameter(description = "Input path", required = true)
	public void setProblemPath(String problemPath) {
		this.problemPath = problemPath;
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

	public void run() throws IOException {
		var problem = addScopeConstraints(loader.loadFile(problemPath));
		var generator = generatorFactory.createGenerator(problem);
		generator.setRandomSeed(randomSeed);
		generator.generate();
		var solution = generator.serializeSolution();
		var solutionResource = solution.eResource();
		var saveOptions = Map.of();
		if (outputPath == null || outputPath.equals("-")) {
			printSolution(solutionResource, saveOptions);
		} else {
			try (var outputStream = new FileOutputStream(outputPath)) {
				solutionResource.save(outputStream, saveOptions);
			}
		}
	}

	private Problem addScopeConstraints(Problem problem) throws IOException {
		var allScopes = new ArrayList<>(scopes);
		allScopes.addAll(overrideScopes);
		if (allScopes.isEmpty()) {
			return problem;
		}
		int originalStatementCount = problem.getStatements().size();
		var builder = new StringBuilder();
		var problemResource = problem.eResource();
		try (var outputStream = new ByteArrayOutputStream()) {
			problemResource.save(outputStream, Map.of());
			builder.append(outputStream.toString(StandardCharsets.UTF_8));
		}
		builder.append('\n');
		for (var scope : allScopes) {
			builder.append("scope ").append(scope).append(".\n");
		}
		var modifiedProblem = loader.loadString(builder.toString(), problemResource.getURI());
		var modifiedStatements = modifiedProblem.getStatements();
		int modifiedStatementCount = modifiedStatements.size();
		if (modifiedStatementCount != originalStatementCount + allScopes.size()) {
			throw new IllegalStateException("Failed to parse scope constraints");
		}
		// Override scopes remove any scope constraint from the original problem with the same target type.
		var overriddenScopes = new HashSet<Relation>();
		for (int i = modifiedStatementCount - overrideScopes.size(); i < modifiedStatementCount; i++) {
			var statement = modifiedStatements.get(i);
			if (!(statement instanceof ScopeDeclaration scopeDeclaration)) {
				throw new IllegalStateException("Invalid scope constraint: " + statement);
			}
			for (var typeScope : scopeDeclaration.getTypeScopes()) {
				overriddenScopes.add(typeScope.getTargetType());
			}
		}
		int statementIndex = 0;
		var iterator = modifiedStatements.iterator();
		// Scope overrides only affect type scopes from the original problem and leave type scopes added on the
		// command line intact.
		while (statementIndex < originalStatementCount && iterator.hasNext()) {
			var statement = iterator.next();
			if (statement instanceof ScopeDeclaration scopeDeclaration) {
				var typeScopes = scopeDeclaration.getTypeScopes();
				typeScopes.removeIf(typeScope -> overriddenScopes.contains(typeScope.getTargetType()));
				// Scope declarations with no type scopes are invalid, so we have to remove them.
				if (typeScopes.isEmpty()) {
					iterator.remove();
				}
			}
			statementIndex++;
		}
		return modifiedProblem;
	}

	// We deliberately write to the standard output if no output path is specified.
	@SuppressWarnings("squid:S106")
	private void printSolution(Resource solutionResource, Map<?, ?> saveOptions) throws IOException {
		solutionResource.save(System.out, saveOptions);
	}
}
