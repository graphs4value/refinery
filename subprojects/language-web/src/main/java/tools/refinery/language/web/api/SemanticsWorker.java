/*
 * SPDX-FileCopyrightText: 2025 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.web.api;

import com.google.inject.Inject;
import org.jetbrains.annotations.Nullable;
import tools.refinery.generator.ModelSemantics;
import tools.refinery.generator.ModelSemanticsFactory;
import tools.refinery.generator.ProblemLoader;
import tools.refinery.language.model.problem.Problem;
import tools.refinery.language.web.api.dto.RefineryResponse;
import tools.refinery.language.web.api.dto.SemanticsRequest;
import tools.refinery.language.web.api.dto.SemanticsSuccessResult;
import tools.refinery.language.web.api.sink.ResponseSink;
import tools.refinery.language.web.api.util.OutputSerializer;
import tools.refinery.language.web.api.util.TimeoutManager;
import tools.refinery.language.web.xtext.server.ThreadPoolExecutorServiceProvider;

import java.io.IOException;
import java.time.Duration;
import java.util.List;

public class SemanticsWorker extends ScheduledWorker<SemanticsRequest> {
	@Inject
	private TimeoutManager timeoutManager;

	@Inject
	private ProblemLoader problemLoader;

	@Inject
	private ModelSemanticsFactory modelSemanticsFactory;

	@Inject
	private OutputSerializer outputSerializer;

	@Override
	protected String getExecutorServiceKey() {
		return ThreadPoolExecutorServiceProvider.SEMANTICS_EXECUTOR;
	}

	@Override
	protected Duration getTimeout() {
		return timeoutManager.getModelSemanticsTimeout();
	}

	@Override
	protected void initialize(SemanticsRequest request, ResponseSink responseSink) {
		super.initialize(request, responseSink);
		var cancellationToken = getCancellationToken();
		problemLoader.cancellationToken(cancellationToken);
		modelSemanticsFactory.cancellationToken(cancellationToken);
		modelSemanticsFactory.concretize(false);
		outputSerializer.setCancellationToken(cancellationToken);
	}

	@Override
	protected void run() throws IOException {
		var request = getRequest();
		if (!request.getFormat().getJson().isEnabled()) {
			var message = "Model semantics only supports JSON output";
			setResponse(new RefineryResponse.RequestError(message, List.of(
					new RefineryResponse.RequestError.Detail("$.format.json.enabled", message))));
			return;
		}
		timeoutManager.markSemanticsAsLoaded();
		var problem = loadProblem();
		if (problem == null) {
			return;
		}
		var semantics = createSemantics(problem);
		saveModel(semantics);
	}

	private @Nullable Problem loadProblem() throws IOException {
		var request = getRequest();
		return problemLoader.loadString(request.getInput().getSource());
	}

	private ModelSemantics createSemantics(Problem problem) {
		checkCancelled();
		var request = getRequest();
		var jsonFormat = request.getFormat().getJson();
		modelSemanticsFactory.keepNonExistingObjects(jsonFormat.getNonExistingObjects().isKeep());
		modelSemanticsFactory.keepShadowPredicates(jsonFormat.getShadowPredicates().isKeep());
		return modelSemanticsFactory.tryCreateSemantics(problem);
	}

	private void saveModel(ModelSemantics semantics) {
		checkCancelled();
		var issues = outputSerializer.getIssues(semantics);
		var json = outputSerializer.savePartialInterpretation(semantics);
		setResponse(new RefineryResponse.Success(new SemanticsSuccessResult(issues, json)));
	}
}
