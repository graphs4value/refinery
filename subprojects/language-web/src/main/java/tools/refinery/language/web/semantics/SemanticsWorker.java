/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.web.semantics;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import org.eclipse.emf.common.util.Diagnostic;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.service.OperationCanceledManager;
import org.eclipse.xtext.util.CancelIndicator;
import org.eclipse.xtext.validation.CheckType;
import org.eclipse.xtext.validation.FeatureBasedDiagnostic;
import org.eclipse.xtext.validation.IDiagnosticConverter;
import org.eclipse.xtext.validation.Issue;
import org.eclipse.xtext.web.server.validation.ValidationResult;
import tools.refinery.language.model.problem.Problem;
import tools.refinery.language.semantics.metadata.MetadataCreator;
import tools.refinery.language.semantics.model.ModelInitializer;
import tools.refinery.language.semantics.model.SemanticsUtils;
import tools.refinery.language.semantics.model.TracedException;
import tools.refinery.store.dse.propagation.PropagationAdapter;
import tools.refinery.store.map.Cursor;
import tools.refinery.store.model.Model;
import tools.refinery.store.model.ModelStore;
import tools.refinery.store.query.viatra.ViatraModelQueryAdapter;
import tools.refinery.store.reasoning.ReasoningAdapter;
import tools.refinery.store.reasoning.ReasoningStoreAdapter;
import tools.refinery.store.reasoning.literal.Concreteness;
import tools.refinery.store.reasoning.representation.PartialRelation;
import tools.refinery.store.reasoning.translator.TranslationException;
import tools.refinery.store.reasoning.translator.multiobject.MultiObjectTranslator;
import tools.refinery.store.tuple.Tuple;
import tools.refinery.viatra.runtime.CancellationToken;

import java.util.ArrayList;
import java.util.TreeMap;
import java.util.concurrent.Callable;

class SemanticsWorker implements Callable<SemanticsResult> {
	private static final String DIAGNOSTIC_ID = "tools.refinery.language.semantics.SemanticError";

	@Inject
	private SemanticsUtils semanticsUtils;

	@Inject
	private OperationCanceledManager operationCanceledManager;

	@Inject
	private IDiagnosticConverter diagnosticConverter;

	@Inject
	private ModelInitializer initializer;

	@Inject
	private MetadataCreator metadataCreator;

	private Problem problem;

	private CancellationToken cancellationToken;

	public void setProblem(Problem problem, CancelIndicator parentIndicator) {
		this.problem = problem;
		cancellationToken = () -> {
			if (Thread.interrupted() || parentIndicator.isCanceled()) {
				operationCanceledManager.throwOperationCanceledException();
			}
		};
	}

	@Override
	public SemanticsResult call() {
		var builder = ModelStore.builder()
				.with(ViatraModelQueryAdapter.builder()
						.cancellationToken(cancellationToken))
				.with(PropagationAdapter.builder())
				.with(ReasoningAdapter.builder()
						.requiredInterpretations(Concreteness.PARTIAL));
		cancellationToken.checkCancelled();
		try {
			var modelSeed = initializer.createModel(problem, builder);
			cancellationToken.checkCancelled();
			metadataCreator.setInitializer(initializer);
			cancellationToken.checkCancelled();
			var nodesMetadata = metadataCreator.getNodesMetadata();
			cancellationToken.checkCancelled();
			var relationsMetadata = metadataCreator.getRelationsMetadata();
			cancellationToken.checkCancelled();
			var store = builder.build();
			cancellationToken.checkCancelled();
			var cancellableModelSeed = CancellableSeed.wrap(cancellationToken, modelSeed);
			var model = store.getAdapter(ReasoningStoreAdapter.class).createInitialModel(cancellableModelSeed);
			cancellationToken.checkCancelled();
			var partialInterpretation = getPartialInterpretation(initializer, model);

			return new SemanticsSuccessResult(nodesMetadata, relationsMetadata, partialInterpretation);
		} catch (TracedException e) {
			return getTracedErrorResult(e.getSourceElement(), e.getMessage());
		} catch (TranslationException e) {
			var sourceElement = initializer.getInverseTrace(e.getPartialSymbol());
			return getTracedErrorResult(sourceElement, e.getMessage());
		}
	}

	private JsonObject getPartialInterpretation(ModelInitializer initializer, Model model) {
		var adapter = model.getAdapter(ReasoningAdapter.class);
		var json = new JsonObject();
		for (var entry : initializer.getRelationTrace().entrySet()) {
			var relation = entry.getKey();
			var partialSymbol = entry.getValue();
			var tuples = getTuplesJson(adapter, partialSymbol);
			var name = semanticsUtils.getName(relation).orElse(partialSymbol.name());
			json.add(name, tuples);
			cancellationToken.checkCancelled();
		}
		json.add("builtin::count", getCountJson(model));
		return json;
	}

	private static JsonArray getTuplesJson(ReasoningAdapter adapter, PartialRelation partialSymbol) {
		var interpretation = adapter.getPartialInterpretation(Concreteness.PARTIAL, partialSymbol);
		var cursor = interpretation.getAll();
		return getTuplesJson(cursor);
	}

	private static JsonArray getTuplesJson(Cursor<Tuple, ?> cursor) {
		var map = new TreeMap<Tuple, Object>();
		while (cursor.move()) {
			map.put(cursor.getKey(), cursor.getValue());
		}
		var tuples = new JsonArray();
		for (var entry : map.entrySet()) {
			tuples.add(toArray(entry.getKey(), entry.getValue()));
		}
		return tuples;
	}

	private static JsonArray toArray(Tuple tuple, Object value) {
		int arity = tuple.getSize();
		var json = new JsonArray(arity + 1);
		for (int i = 0; i < arity; i++) {
			json.add(tuple.get(i));
		}
		json.add(value.toString());
		return json;
	}

	private static JsonArray getCountJson(Model model) {
		var interpretation = model.getInterpretation(MultiObjectTranslator.COUNT_STORAGE);
		var cursor = interpretation.getAll();
		return getTuplesJson(cursor);
	}

	private SemanticsResult getTracedErrorResult(EObject sourceElement, String message) {
		if (sourceElement == null || !problem.eResource().equals(sourceElement.eResource())) {
			return new SemanticsInternalErrorResult(message);
		}
		var diagnostic = new FeatureBasedDiagnostic(Diagnostic.ERROR, message, sourceElement, null, 0,
				CheckType.EXPENSIVE, DIAGNOSTIC_ID);
		var xtextIssues = new ArrayList<Issue>();
		diagnosticConverter.convertValidatorDiagnostic(diagnostic, xtextIssues::add);
		var issues = xtextIssues.stream()
				.map(issue -> new ValidationResult.Issue(issue.getMessage(), "error", issue.getLineNumber(),
						issue.getColumn(), issue.getOffset(), issue.getLength()))
				.toList();
		return new SemanticsIssuesResult(issues);
	}
}
