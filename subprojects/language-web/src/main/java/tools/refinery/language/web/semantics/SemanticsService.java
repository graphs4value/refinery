/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.web.semantics;

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
		if (!(doc instanceof PushWebDocument pushDoc)) {
			throw new IllegalArgumentException("Unexpected IXtextWebDocument: " + doc);
		}
		if (hasError(pushDoc, cancelIndicator)) {
			return null;
		}
		var problem = getProblem(doc);
		if (problem == null) {
			return new SemanticsResult(SemanticsModelResult.EMPTY);
		}
		var worker = workerProvider.get();
		worker.setProblem(problem, pushDoc.isConcretize(), cancelIndicator);
		var future = executorService.submit(worker);
		var result = handleFuture(future);
		if (LOG.isTraceEnabled()) {
			long end = System.currentTimeMillis();
			LOG.trace("Computed semantics for {} ({}) in {}ms", doc.getResourceId(), doc.getStateId(),
					end - start);
		}
		return result;
	}

	private SemanticsResult handleFuture(Future<SemanticsResult> future) {
		boolean warmedUpCurrently = warmedUp.get();
		long timeout = warmedUpCurrently ? timeoutMs : warmupTimeoutMs;
		try {
			var result = future.get(timeout, TimeUnit.MILLISECONDS);
			if (!warmedUpCurrently) {
				warmedUp.set(true);
			}
			return result;
		} catch (InterruptedException e) {
			future.cancel(true);
			var message = "Semantics service interrupted";
			LOG.error(message, e);
			Thread.currentThread().interrupt();
			return new SemanticsResult(message);
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
			return new SemanticsResult(message);
		} catch (TimeoutException e) {
			future.cancel(true);
			if (!warmedUpCurrently) {
				warmedUp.set(true);
			}
			LOG.trace("Semantics service timeout", e);
			return new SemanticsResult("Partial interpretation timed out");
		}
	}

	private boolean hasError(PushWebDocument pushDoc, CancelIndicator cancelIndicator) {
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
		var model = contents.getFirst();
		if (!(model instanceof Problem problem)) {
			return null;
		}
		return problem;
	}
}
