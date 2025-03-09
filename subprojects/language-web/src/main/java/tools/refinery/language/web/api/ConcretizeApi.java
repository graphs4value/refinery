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
import tools.refinery.language.web.api.dto.SemanticsRequest;
import tools.refinery.language.web.api.sink.AsyncResponseSink;
import tools.refinery.language.web.api.sink.SseResponseSink;

@Path("/v1/concretize")
public class ConcretizeApi {
	private final ConcretizeWorker worker;

	@Inject
	public ConcretizeApi(ConcretizeWorker worker) {
		this.worker = worker;
	}

	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public void generate(@Valid SemanticsRequest request, @Suspended AsyncResponse asyncResponse) {
		var responseSink = new AsyncResponseSink(asyncResponse);
		// Fire and forget, because the worker will handle its own exceptions.
		worker.schedule(request, responseSink);
	}

	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.SERVER_SENT_EVENTS)
	public void generate(@Valid SemanticsRequest request, @Context SseEventSink eventSink, @Context Sse sse)
			throws InterruptedException {
		var responseSink = new SseResponseSink(eventSink, sse);
		worker.schedule(request, responseSink);
		responseSink.loop(worker);
	}
}
