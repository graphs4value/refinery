/*
 * SPDX-FileCopyrightText: 2025 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.web.api;

import tools.refinery.generator.ModelGenerator;
import tools.refinery.language.web.api.dto.GenerateRequest;
import tools.refinery.language.web.api.dto.RefineryResponse;
import tools.refinery.language.web.api.sink.ResponseSink;

import java.io.IOException;

public class GenerateWorker extends AbstractGenerateWorker<GenerateRequest> {
	@Override
	protected void initialize(GenerateRequest request, ResponseSink responseSink) {
		if (request.isMany()) {
			throw new IllegalArgumentException("Use GenerateManyWorker to generate multiple models.");
		}
		super.initialize(request, responseSink);
	}

	@Override
	protected void runModelGenerator(ModelGenerator generator) throws IOException {
		updateStatusString("Generating model");
		generator.generate();
		updateStatusString("Saving generated model");
		setResponse(new RefineryResponse.Success(saveModel(generator)));
	}
}
