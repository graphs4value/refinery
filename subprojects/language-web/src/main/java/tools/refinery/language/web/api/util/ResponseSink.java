/*
 * SPDX-FileCopyrightText: 2025 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.web.api.util;

import jakarta.ws.rs.core.Response;
import tools.refinery.language.web.api.dto.RefineryResponse;

public interface ResponseSink {
	default void setResponse(RefineryResponse response) {
		setResponse(response.getStatus(), response);
	}

	default void setResponse(Response.Status status, RefineryResponse response) {
		setResponse(status.getStatusCode(), response);
	}

	void setResponse(int statusCode, RefineryResponse response);

	void updateStatus(String status);

	boolean isCancelled();
}
