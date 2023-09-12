/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.web.semantics;

import com.google.gson.JsonObject;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import org.eclipse.xtext.ide.ExecutorServiceProvider;
import org.eclipse.xtext.service.OperationCanceledManager;
import org.eclipse.xtext.util.CancelIndicator;
import org.eclipse.xtext.web.server.model.AbstractCachedService;
import org.eclipse.xtext.web.server.model.IXtextWebDocument;
import org.eclipse.xtext.web.server.validation.ValidationService;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.refinery.language.model.problem.Problem;
import tools.refinery.language.web.xtext.server.push.PushWebDocument;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

@Singleton
public class SemanticsService extends AbstractCachedService<SemanticsResult> {
	public static final String SEMANTICS_EXECUTOR = "semantics";

	private static final Logger LOG = LoggerFactory.getLogger(SemanticsService.class);

	@Inject
	private Provider<SemanticsWorker> workerProvider;

	@Inject
	private OperationCanceledManager operationCanceledManager;

	@Inject
	private ValidationService validationService;

	private ExecutorService executorService;

	private final long timeoutMs;

	private final long warmupTimeoutMs;

	private final AtomicBoolean warmedUp = new AtomicBoolean(false);

	public SemanticsService() {
		timeoutMs = getTimeout("REFINERY_SEMANTICS_TIMEOUT_MS").orElse(1000L);
		warmupTimeoutMs = getTimeout("REFINERY_SEMANTICS_WARMUP_TIMEOUT_MS").orElse(timeoutMs * 2);
	}

	public static Optional<Long> getTimeout(String name) {
		return Optional.ofNullable(System.getenv(name)).map(Long::parseUnsignedLong);
	}

	@Inject
	public void setExecutorServiceProvider(ExecutorServiceProvider provider) {
		executorService = provider.get(SEMANTICS_EXECUTOR);
	}

	@Override
	public SemanticsResult compute(IXtextWebDocument doc, CancelIndicator cancelIndicator) {
		long start = 0;
		if (LOG.isTraceEnabled()) {
			start = System.currentTimeMillis();
		}
		if (hasError(doc, cancelIndicator)) {
			return null;
		}
		var problem = getProblem(doc);
		if (problem == null) {
			return new SemanticsSuccessResult(List.of(), List.of(), new JsonObject());
		}
		var worker = workerProvider.get();
		worker.setProblem(problem, cancelIndicator);
		var future = executorService.submit(worker);
		boolean warmedUpCurrently = warmedUp.get();
		long timeout = warmedUpCurrently ? timeoutMs : warmupTimeoutMs;
		SemanticsResult result = null;
		try {
			result = future.get(timeout, TimeUnit.MILLISECONDS);
			if (!warmedUpCurrently) {
				warmedUp.set(true);
			}
		} catch (InterruptedException e) {
			future.cancel(true);
			LOG.error("Semantics service interrupted", e);
			Thread.currentThread().interrupt();
		} catch (ExecutionException e) {
			operationCanceledManager.propagateAsErrorIfCancelException(e.getCause());
			LOG.debug("Error while computing semantics", e);
			if (e.getCause() instanceof Error error) {
				throw error;
			}
			String message = e.getMessage();
			if (message == null) {
				message = "Partial interpretation error";
			}
			return new SemanticsInternalErrorResult(message);
		} catch (TimeoutException e) {
			future.cancel(true);
			if (!warmedUpCurrently) {
				warmedUp.set(true);
			}
			LOG.trace("Semantics service timeout", e);
			return new SemanticsInternalErrorResult("Partial interpretation timed out");
		}
		if (LOG.isTraceEnabled()) {
			long end = System.currentTimeMillis();
			LOG.trace("Computed semantics for {} ({}) in {}ms", doc.getResourceId(), doc.getStateId(),
					end - start);
		}
		return result;
	}

	private boolean hasError(IXtextWebDocument doc, CancelIndicator cancelIndicator) {
		if (!(doc instanceof PushWebDocument pushDoc)) {
			throw new IllegalArgumentException("Unexpected IXtextWebDocument: " + doc);
		}
		var validationResult = pushDoc.getCachedServiceResult(validationService, cancelIndicator, true);
		return validationResult.getIssues().stream()
				.anyMatch(issue -> "error".equals(issue.getSeverity()));
	}

	@Nullable
	private Problem getProblem(IXtextWebDocument doc) {
		var contents = doc.getResource().getContents();
		if (contents.isEmpty()) {
			return null;
		}
		var model = contents.get(0);
		if (!(model instanceof Problem problem)) {
			return null;
		}
		return problem;
	}
}
