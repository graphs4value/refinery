/*
 * SPDX-FileCopyrightText: 2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.generator.cli.utils;

import org.eclipse.emf.ecore.resource.Resource;
import tools.refinery.generator.ModelFacade;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Map;

public class CliProblemSerializer {
	public void saveModel(ModelFacade modelFacade, String outputPath) throws IOException {
		saveModel(modelFacade, outputPath, true);
	}

	public void saveModel(ModelFacade modelFacade, String outputPath,
						  boolean allowStandardOutput) throws IOException {
		var problem = modelFacade.serialize();
		var resource = problem.eResource();
		var saveOptions = Map.of();
		if (CliUtils.isStandardStream(outputPath)) {
			if (!allowStandardOutput) {
				throw new IllegalArgumentException("Refusing to save model to standard output '" +
						CliUtils.STANDARD_OUTPUT_PATH + "'");
			}
			printSolution(resource, saveOptions);
		} else {
			try (var outputStream = new FileOutputStream(outputPath)) {
				resource.save(outputStream, saveOptions);
			}
		}
	}

	// We deliberately write to the standard output if no output path is specified.
	@SuppressWarnings("squid:S106")
	private static void printSolution(Resource solutionResource, Map<?, ?> saveOptions) throws IOException {
		solutionResource.save(System.out, saveOptions);
	}
}
