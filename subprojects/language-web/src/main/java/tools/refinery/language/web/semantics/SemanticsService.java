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
import java.util.concurrent.*;

@Singleton
public class SemanticsService extends AbstractCachedService<SemanticsResult> {
	private static final Logger LOG = LoggerFactory.getLogger(SemanticsService.class);

	@Inject
	private Provider<SemanticsWorker> workerProvider;

	@Inject
	private OperationCanceledManager operationCanceledManager;

	@Inject
	private ValidationService validationService;

	private ExecutorService executorService;

	@Inject
	public void setExecutorServiceProvider(ExecutorServiceProvider provider) {
		executorService = provider.get(this.getClass().getName());
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
		SemanticsResult result = null;
		try {
			result = future.get(2, TimeUnit.SECONDS);
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
