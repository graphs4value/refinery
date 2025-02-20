/*
 * SPDX-FileCopyrightText: 2025 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.web.api.sink;

import jakarta.ws.rs.container.AsyncResponse;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import tools.refinery.language.web.api.dto.RefineryResponse;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class AsyncResponseSink implements ResponseSink {
	private final Lock lock = new ReentrantLock();
	private boolean finished;
	private final AsyncResponse asyncResponse;

	public AsyncResponseSink(AsyncResponse asyncResponse) {
		this.asyncResponse = asyncResponse;
	}

	@Override
	public void setResponse(int statusCode, RefineryResponse response) {
		lock.lock();
		try {
			if (finished) {
				return;
			}
			finished = asyncResponse.resume(Response.status(statusCode)
					.type(MediaType.APPLICATION_JSON_TYPE)
					.entity(response)
					.build());
		} finally {
			lock.unlock();
		}
	}

	@Override
	public void updateStatus(String status) {
		// REST request can't have periodic status updates.
	}

	@Override
	public boolean isCancelled() {
		return asyncResponse.isCancelled();
	}
}
