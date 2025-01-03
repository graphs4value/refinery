/*
 * SPDX-FileCopyrightText: 2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.web.api;

import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.container.AsyncResponse;
import jakarta.ws.rs.container.Suspended;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.sse.Sse;
import jakarta.ws.rs.sse.SseEventSink;
import org.eclipse.xtext.ide.ExecutorServiceProvider;
import tools.refinery.language.web.api.dto.GenerateRequest;
import tools.refinery.language.web.api.util.AsyncResponseSink;
import tools.refinery.language.web.api.util.SseResponseSink;
import tools.refinery.language.web.generator.ModelGenerationService;

import java.util.concurrent.ExecutorService;

@Path("/v1/generate")
public class GenerateApi {
	private final GenerateWorker worker;
	private final ExecutorService executorService;

	@Inject
	public GenerateApi(GenerateWorker worker, ExecutorServiceProvider executorServiceProvider) {
		this.worker = worker;
		executorService = executorServiceProvider.get(ModelGenerationService.MODEL_GENERATION_EXECUTOR);
	}

	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public void generate(@Valid GenerateRequest request, @Suspended AsyncResponse asyncResponse) {
		var responseSink = new AsyncResponseSink(asyncResponse);
		worker.initialize(request, responseSink);
		// Fire and forget, because the worker will handle its own exceptions.
		executorService.submit(worker);
	}

	@POST
	@Path("/stream")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.SERVER_SENT_EVENTS)
	public void generate(@Valid GenerateRequest request, @Context SseEventSink eventSink, @Context Sse sse)
			throws InterruptedException {
		var responseSink = new SseResponseSink(eventSink, sse);
		worker.initialize(request, responseSink);
		var future = executorService.submit(worker);
		responseSink.loop(future);
	}
}
