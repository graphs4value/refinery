/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.web.semantics;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import org.eclipse.xtext.service.OperationCanceledManager;
import org.eclipse.xtext.util.CancelIndicator;
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
import tools.refinery.viatra.runtime.CancellationToken;

import java.util.Arrays;
import java.util.List;
import java.util.TreeMap;
import java.util.concurrent.Callable;

class SemanticsWorker implements Callable<SemanticsResult> {
	private static final Logger LOG = LoggerFactory.getLogger(SemanticsWorker.class);

	@Inject
	private SemanticsUtils semanticsUtils;

	@Inject
	private OperationCanceledManager operationCanceledManager;

	@Inject
	private ModelInitializer initializer;

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
				.with(ReasoningAdapter.builder()
						.requiredInterpretations(Concreteness.PARTIAL));
		cancellationToken.checkCancelled();
		try {
			var modelSeed = initializer.createModel(problem, builder);
			cancellationToken.checkCancelled();
			var nodeTrace = getNodeTrace(initializer);
			cancellationToken.checkCancelled();
			var store = builder.build();
			cancellationToken.checkCancelled();
			var model = store.getAdapter(ReasoningStoreAdapter.class).createInitialModel(modelSeed);
			cancellationToken.checkCancelled();
			var partialInterpretation = getPartialInterpretation(initializer, model);

			return new SemanticsSuccessResult(nodeTrace, partialInterpretation);
		} catch (RuntimeException e) {
			LOG.debug("Error while computing semantics", e);
			var message = e.getMessage();
			return new SemanticsErrorResult(message == null ? "Partial interpretation error" : e.getMessage());
		}
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
