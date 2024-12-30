/*
 * SPDX-FileCopyrightText: 2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.web.api;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.StreamingOutput;
import jakarta.ws.rs.sse.Sse;
import jakarta.ws.rs.sse.SseEventSink;
import org.eclipse.jetty.io.EofException;

import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

@Path("/v1/semantics")
public class SemanticsApi {
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public StreamingOutput getSemantics() {
		return outputStream -> {
			try (var writer = new BufferedWriter(new OutputStreamWriter(outputStream, StandardCharsets.UTF_8))) {
				try {
					while (true) {
						writer.write("\r\n");
						writer.flush();
						Thread.sleep(Duration.ofSeconds(1));
					}
				} catch (EofException e) {
					System.out.println("Client closed the connection");
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
			}
		};
	}

	@POST
	@Path("/stream")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.SERVER_SENT_EVENTS)
	public void getSemanticsStream(@Context SseEventSink eventSink, @Context Sse sse) {
		while (!eventSink.isClosed()) {
			var event = sse.newEventBuilder()
					.mediaType(MediaType.APPLICATION_JSON_TYPE)
					.data(new Message("Hello, World!", 1000))
					.build();
			eventSink.send(event);
			try {
				Thread.sleep(Duration.ofSeconds(1));
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		}
		System.out.println("SSE closed");
	}

	record Message(String value, int priority) {
	}
}
