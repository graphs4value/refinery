/*
 * SPDX-FileCopyrightText: 2025 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.web.api.util;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.eclipse.xtext.web.server.validation.ValidationResult;
import tools.refinery.generator.ModelFacade;
import tools.refinery.generator.ModelFacadeResult;
import tools.refinery.generator.RefineryDiagnostics;
import tools.refinery.language.web.api.dto.JsonOutput;
import tools.refinery.language.web.api.dto.RefineryResponse;
import tools.refinery.language.web.semantics.PartialInterpretation2Json;
import tools.refinery.store.reasoning.ReasoningAdapter;
import tools.refinery.store.util.CancellationToken;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@Singleton
public class OutputSerializer {
	/**
	 * To avoid excessive memory usage by the Xtext serializer, we limit the number of objects in a serialized model.
	 * <p>
	 * Model with more objects than this limit will only be returned as JSON.
	 * </p>
	 */
	private static final int MAX_SERIALIZED_MODEL_SIZE = 200;

	@Inject
	private PartialInterpretation2Json partialInterpretation2Json;

	@Inject
	private RefineryDiagnostics refineryDiagnostics;

	private CancellationToken cancellationToken;

	public void setCancellationToken(CancellationToken cancellationToken) {
		this.cancellationToken = cancellationToken;
	}

	private void checkCancelled() {
		cancellationToken.checkCancelled();
	}

	public String saveSource(ModelFacade facade) throws IOException {
		checkCancelled();
		if (isLarge(facade)) {
			return null;
		}
		checkCancelled();
		var serializedSolutionOption = facade.trySerialize();
		if (serializedSolutionOption.isEmpty()) {
			return null;
		}
		var serializedSolution = serializedSolutionOption.get();
		checkCancelled();
		try (var outputStream = new ByteArrayOutputStream()) {
			serializedSolution.eResource().save(outputStream, Map.of());
			return outputStream.toString(StandardCharsets.UTF_8);
		}
	}

	private boolean isLarge(ModelFacade facade) {
		var cursor = facade.getPartialInterpretation(ReasoningAdapter.EXISTS_SYMBOL).getAll();
		for (int i = 0; i < MAX_SERIALIZED_MODEL_SIZE; i++) {
			if (!cursor.move()) {
				return false;
			}
		}
		return true;
	}

	public JsonOutput savePartialInterpretation(ModelFacade facade) {
		checkCancelled();
		var nodes = facade.getNodesMetadata().list();
		checkCancelled();
		var relations = facade.getRelationsMetadata();
		checkCancelled();
		var partialInterpretation = partialInterpretation2Json.getPartialInterpretation(facade, cancellationToken);
		return new JsonOutput(nodes, relations, partialInterpretation);
	}

	public List<ValidationResult.Issue> getIssues(ModelFacade facade) {
		var initializationResult = facade.getInitializationResult();
		if (!(initializationResult instanceof ModelFacadeResult.Rejected rejectedResult)) {
			return List.of();
		}
		var trace = facade.getProblemTrace();
		var errors = refineryDiagnostics.convertIssues(rejectedResult, trace);
		return RefineryResponse.InvalidProblem.translateIssues(errors, trace.getProblem().eResource().getURI());
	}
}
