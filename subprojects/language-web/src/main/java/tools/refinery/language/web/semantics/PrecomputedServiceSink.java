/*
 * SPDX-FileCopyrightText: 2025 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.web.semantics;

import org.eclipse.xtext.util.CancelIndicator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.refinery.language.web.api.ScheduledWorker;
import tools.refinery.language.web.api.dto.RefineryResponse;
import tools.refinery.language.web.api.sink.ResponseSink;

import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;

class PrecomputedServiceSink implements ResponseSink {
	private static final Logger LOG = LoggerFactory.getLogger(PrecomputedServiceSink.class);

	private final CancelIndicator cancelIndicator;
	private RefineryResponse response;

	PrecomputedServiceSink(CancelIndicator cancelIndicator) {
		this.cancelIndicator = cancelIndicator;
	}

	@Override
	public void setResponse(int statusCode, RefineryResponse response) {
		this.response = response;
	}

	@Override
	public void updateStatus(String status) {
		throw new UnsupportedOperationException("Cannot update status in precomputed service");
	}

	@Override
	public boolean isCancelled() {
		return cancelIndicator.isCanceled();
	}

	public RefineryResponse getResponse(ScheduledWorker<?> worker) {
		try {
			worker.poll();
		} catch (ExecutionException e) {
			// This should never happen, because the worker will handle its own exceptions.
			LOG.error("Uncaught exception in worker", e);
			response = new RefineryResponse.ServerError("Internal error");
		} catch (InterruptedException e) {
			LOG.debug("Worker interrupted", e);
			Thread.currentThread().interrupt();
			response = new RefineryResponse.Cancelled("Computation interrupted by server");
		} catch (CancellationException e) {
			response = new RefineryResponse.Cancelled("Computation interrupted by server");
		}
		if (isCancelled()) {
			return null;
		}
		return response;
	}
}
