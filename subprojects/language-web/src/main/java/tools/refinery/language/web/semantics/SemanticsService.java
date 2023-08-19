/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.web.semantics;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
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

	private final ExecutorService executorService = Executors.newCachedThreadPool();

	@Override
	public SemanticsResult compute(IXtextWebDocument doc, CancelIndicator cancelIndicator) {
		long start = 0;
		if (LOG.isTraceEnabled()) {
			start = System.currentTimeMillis();
		}
		var problem = getProblem(doc, cancelIndicator);
		if (problem == null) {
			return null;
		}
		var worker = workerProvider.get();
		worker.setProblem(problem,cancelIndicator);
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
			throw new IllegalStateException(e);
		} catch (TimeoutException e) {
			future.cancel(true);
			LOG.trace("Semantics service timeout", e);
			return new SemanticsErrorResult("Partial interpretation timed out");
		}
		if (LOG.isTraceEnabled()) {
			long end = System.currentTimeMillis();
			LOG.trace("Computed semantics for {} ({}) in {}ms", doc.getResourceId(), doc.getStateId(),
					end - start);
		}
		return result;
	}

	@Nullable
	private Problem getProblem(IXtextWebDocument doc, CancelIndicator cancelIndicator) {
		if (!(doc instanceof PushWebDocument pushDoc)) {
			throw new IllegalArgumentException("Unexpected IXtextWebDocument: " + doc);
		}
		var validationResult = pushDoc.getCachedServiceResult(validationService, cancelIndicator, true);
		boolean hasError = validationResult.getIssues().stream()
				.anyMatch(issue -> "error".equals(issue.getSeverity()));
		if (hasError) {
			return null;
		}
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
