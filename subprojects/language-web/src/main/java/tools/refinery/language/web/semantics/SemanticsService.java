/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.web.semantics;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import org.eclipse.xtext.service.OperationCanceledManager;
import org.eclipse.xtext.util.CancelIndicator;
import org.eclipse.xtext.web.server.model.AbstractCachedService;
import org.eclipse.xtext.web.server.model.IXtextWebDocument;
import org.eclipse.xtext.web.server.model.XtextWebDocument;
import org.eclipse.xtext.web.server.validation.ValidationService;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.refinery.language.model.problem.Problem;
import tools.refinery.language.semantics.model.ModelInitializer;
import tools.refinery.language.semantics.model.SemanticsUtils;
import tools.refinery.store.model.Model;
import tools.refinery.store.model.ModelStore;
import tools.refinery.store.query.viatra.ViatraModelQueryAdapter;
import tools.refinery.store.reasoning.ReasoningAdapter;
import tools.refinery.store.reasoning.ReasoningStoreAdapter;
import tools.refinery.store.reasoning.literal.Concreteness;
import tools.refinery.store.reasoning.representation.PartialRelation;
import tools.refinery.store.representation.TruthValue;
import tools.refinery.store.tuple.Tuple;

import java.util.Arrays;
import java.util.List;
import java.util.TreeMap;

@Singleton
public class SemanticsService extends AbstractCachedService<SemanticsResult> {
	private static final Logger LOG = LoggerFactory.getLogger(SemanticsService.class);

	@Inject
	private SemanticsUtils semanticsUtils;

	@Inject
	private ValidationService validationService;

	@Inject
	private Provider<ModelInitializer> initializerProvider;

	@Inject
	private OperationCanceledManager operationCanceledManager;

	@Override
	public SemanticsResult compute(IXtextWebDocument doc, CancelIndicator cancelIndicator) {
		long start = 0;
		if (LOG.isTraceEnabled()) {
			start = System.currentTimeMillis();
		}
		Problem problem = getProblem(doc, cancelIndicator);
		if (problem == null) {
			return null;
		}
		var initializer = initializerProvider.get();
		var builder = ModelStore.builder()
				.with(ViatraModelQueryAdapter.builder())
				.with(ReasoningAdapter.builder()
						.requiredInterpretations(Concreteness.PARTIAL));
		operationCanceledManager.checkCanceled(cancelIndicator);
		try {
			var modelSeed = initializer.createModel(problem, builder);
			operationCanceledManager.checkCanceled(cancelIndicator);
			var nodeTrace = getNodeTrace(initializer);
			operationCanceledManager.checkCanceled(cancelIndicator);
			var store = builder.build();
			operationCanceledManager.checkCanceled(cancelIndicator);
			var model = store.getAdapter(ReasoningStoreAdapter.class).createInitialModel(modelSeed);
			operationCanceledManager.checkCanceled(cancelIndicator);
			var partialInterpretation = getPartialInterpretation(initializer, model, cancelIndicator);
			if (LOG.isTraceEnabled()) {
				long end = System.currentTimeMillis();
				LOG.trace("Computed semantics for {} ({}) in {}ms", doc.getResourceId(), doc.getStateId(),
						end - start);
			}
			return new SemanticsSuccessResult(nodeTrace, partialInterpretation);
		} catch (RuntimeException e) {
			LOG.debug("Error while computing semantics", e);
			return new SemanticsErrorResult(e.getMessage());
		}
	}

	@Nullable
	private Problem getProblem(IXtextWebDocument doc, CancelIndicator cancelIndicator) {
		if (!(doc instanceof XtextWebDocument webDoc)) {
			throw new IllegalArgumentException("Unexpected IXtextWebDocument: " + doc);
		}
		var validationResult = webDoc.getCachedServiceResult(validationService, cancelIndicator, true);
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

	private List<String> getNodeTrace(ModelInitializer initializer) {
		var nodeTrace = new String[initializer.getNodeCount()];
		for (var entry : initializer.getNodeTrace().keyValuesView()) {
			var node = entry.getOne();
			var index = entry.getTwo();
			nodeTrace[index] = semanticsUtils.getName(node).orElse(null);
		}
		return Arrays.asList(nodeTrace);
	}

	private JsonObject getPartialInterpretation(ModelInitializer initializer, Model model,
												CancelIndicator cancelIndicator) {
		var adapter = model.getAdapter(ReasoningAdapter.class);
		var json = new JsonObject();
		for (var entry : initializer.getRelationTrace().entrySet()) {
			var relation = entry.getKey();
			var partialSymbol = entry.getValue();
			var tuples = getTuplesJson(adapter, partialSymbol);
			var name = semanticsUtils.getName(relation).orElse(partialSymbol.name());
			json.add(name, tuples);
			operationCanceledManager.checkCanceled(cancelIndicator);
		}
		return json;
	}

	private static JsonArray getTuplesJson(ReasoningAdapter adapter, PartialRelation partialSymbol) {
		var interpretation = adapter.getPartialInterpretation(Concreteness.PARTIAL, partialSymbol);
		var cursor = interpretation.getAll();
		var map = new TreeMap<Tuple, TruthValue>();
		while (cursor.move()) {
			map.put(cursor.getKey(), cursor.getValue());
		}
		var tuples = new JsonArray();
		for (var entry : map.entrySet()) {
			tuples.add(toArray(entry.getKey(), entry.getValue()));
		}
		return tuples;
	}

	private static JsonArray toArray(Tuple tuple, TruthValue value) {
		int arity = tuple.getSize();
		var json = new JsonArray(arity + 1);
		for (int i = 0; i < arity; i++) {
			json.add(tuple.get(i));
		}
		json.add(value.toString());
		return json;
	}
}
