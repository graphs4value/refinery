/*
 * SPDX-FileCopyrightText: 2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.generator.cli.output;

import com.google.gson.Gson;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.eclipse.emf.ecore.resource.Resource;
import tools.refinery.generator.ModelFacade;
import tools.refinery.generator.cli.headless.GraphRenderer;
import tools.refinery.generator.cli.utils.CliUtils;
import tools.refinery.generator.json.PartialModel2Json;
import tools.refinery.generator.json.PartialModelJson;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Singleton
public class CliProblemOutput {
	@Inject
	private PartialModel2Json partialModel2Json;

	@Inject
	private GraphRenderer graphRenderer;

	public void saveModel(OutputOptions options, ModelFacade modelFacade) throws IOException {
		saveModel(options, modelFacade, options.getOutputPath(), true);
	}

	public void saveModel(OutputOptions options, ModelFacade modelFacade, int i) throws IOException {
		var outputPath = options.getOutputPath();
		var pathWithIndex = CliUtils.getFileNameWithIndex(outputPath, i + 1);
		saveModel(options, modelFacade, pathWithIndex, false);
	}

	private void saveModel(OutputOptions outputOptions, ModelFacade modelFacade, String outputPath,
	                       boolean allowStandardOutput) throws IOException {
		switch (outputOptions.getFormat()) {
		case REFINERY -> saveRefinery(modelFacade, outputPath, allowStandardOutput);
		case JSON -> saveJson(modelFacade, outputPath, allowStandardOutput);
		default -> saveGraphical(outputOptions, modelFacade, outputPath, allowStandardOutput);
		}
	}

	private void saveRefinery(ModelFacade modelFacade, String outputPath, boolean allowStandardOutput)
			throws IOException {
		var problem = modelFacade.serialize();
		var resource = problem.eResource();
		var saveOptions = Map.of();
		if (CliUtils.isStandardStream(outputPath)) {
			checkStandardOutputAllowed(allowStandardOutput);
			printRefinery(resource, saveOptions);
		} else {
			try (var outputStream = new FileOutputStream(outputPath)) {
				resource.save(outputStream, saveOptions);
			}
		}
	}

	// We deliberately write to the standard output if no output path is specified.
	@SuppressWarnings("squid:S106")
	private static void printRefinery(Resource solutionResource, Map<?, ?> saveOptions) throws IOException {
		solutionResource.save(System.out, saveOptions);
	}

	private void saveJson(ModelFacade modelFacade, String outputPath, boolean allowStandardOutput) throws IOException {
		var json = partialModel2Json.savePartialInterpretation(modelFacade);
		var gson = GsonUtil.getGson();
		if (CliUtils.isStandardStream(outputPath)) {
			checkStandardOutputAllowed(allowStandardOutput);
			printJson(json, gson);
		} else {
			try (var fileWriter = new FileWriter(outputPath, StandardCharsets.UTF_8)) {
				gson.toJson(json, fileWriter);
			}
		}
	}

	// We deliberately write to the standard output if no output path is specified.
	@SuppressWarnings("squid:S106")
	private static void printJson(PartialModelJson json, Gson gson) {
		gson.toJson(json, System.out);
	}

	private void saveGraphical(OutputOptions outputOptions, ModelFacade modelFacade, String outputPath,
	                           boolean allowStandardOutput) throws IOException {
		var json = partialModel2Json.savePartialInterpretation(modelFacade);
		byte[] jsonBytes = GsonUtil.getGson().toJson(json).getBytes(StandardCharsets.UTF_8);
		saveGraphical(outputOptions, jsonBytes, outputPath, allowStandardOutput);
	}

	public void saveGraphical(OutputOptions outputOptions, byte[] jsonBytes) throws IOException {
		saveGraphical(outputOptions, jsonBytes, outputOptions.getOutputPath(), true);
	}

	private void saveGraphical(OutputOptions outputOptions, byte[] jsonBytes, String outputPath,
	                           boolean allowStandardOutput) throws IOException {
		var imageBytes = graphRenderer.render(outputOptions, jsonBytes);
		if (CliUtils.isStandardStream(outputPath)) {
			checkStandardOutputAllowed(allowStandardOutput);
			printBytes(imageBytes);
		} else {
			try (var outputStream = new FileOutputStream(outputPath)) {
				outputStream.write(imageBytes);
			}
		}
	}

	// We deliberately write to the standard output if no output path is specified.
	@SuppressWarnings("squid:S106")
	private static void printBytes(byte[] bytes) throws IOException {
		System.out.write(bytes);
	}

	private static void checkStandardOutputAllowed(boolean allowStandardOutput) {
		if (!allowStandardOutput) {
			throw new IllegalArgumentException("Refusing to save model to standard output '" +
					CliUtils.STANDARD_OUTPUT_PATH + "'");
		}
	}
}
