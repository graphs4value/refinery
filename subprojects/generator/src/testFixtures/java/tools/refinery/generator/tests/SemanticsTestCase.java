/*
 * SPDX-FileCopyrightText: 2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.generator.tests;

import org.eclipse.collections.api.factory.primitive.IntObjectMaps;
import org.eclipse.collections.api.map.primitive.IntObjectMap;
import org.eclipse.core.runtime.AssertionFailedException;
import tools.refinery.generator.FilteredInterpretation;
import tools.refinery.generator.ModelSemantics;
import tools.refinery.generator.ModelSemanticsFactory;
import tools.refinery.language.model.problem.Problem;
import tools.refinery.language.semantics.ProblemTrace;
import tools.refinery.logic.term.truthvalue.TruthValue;
import tools.refinery.store.map.Cursor;
import tools.refinery.store.reasoning.ReasoningAdapter;
import tools.refinery.store.reasoning.literal.Concreteness;
import tools.refinery.store.reasoning.representation.PartialRelation;
import tools.refinery.store.tuple.Tuple;

import java.util.List;

public record SemanticsTestCase(String name, boolean allowErrors, Problem problem,
								List<SemanticsExpectation> expectations) {
	public void execute(ModelSemanticsFactory semanticsFactory) {
		semanticsFactory.withCandidateInterpretations(needsCandidateInterpretations());
		var semantics = semanticsFactory.createSemantics(problem);
		if (!allowErrors) {
			checkNoErrors(semantics);
		}
		for (var expectation : expectations) {
			expectation.execute(semantics);
		}
	}

	public boolean needsCandidateInterpretations() {
		for (var expectation : expectations) {
			if (expectation.concreteness() == Concreteness.CANDIDATE) {
				return true;
			}
		}
		return false;
	}

	private void checkNoErrors(ModelSemantics semantics) {
		boolean hasError = false;
		var errorsBuilder = new StringBuilder("Errors found in partial model:\n\n");
		var trace = semantics.getProblemTrace();
		IntObjectMap<String> nodeNames = null;
		var existsInterpretation = semantics.getPartialInterpretation(ReasoningAdapter.EXISTS_SYMBOL);
		for (var symbol : trace.getRelationTrace().values()) {
			var interpretation = new FilteredInterpretation<>(semantics.getPartialInterpretation(symbol),
					existsInterpretation);
			var cursor = interpretation.getAll();
			while (cursor.move()) {
				if (!cursor.getValue().isError()) {
					continue;
				}
				hasError = true;
				if (nodeNames == null) {
					nodeNames = getNodeNames(trace);
				}
				appendError(symbol, errorsBuilder, cursor, nodeNames);
			}
		}
		if (hasError) {
			throw new AssertionFailedException(errorsBuilder.toString());
		}
	}

	private IntObjectMap<String> getNodeNames(ProblemTrace trace) {
		var nodeNames = IntObjectMaps.mutable.<String>empty();
		trace.getNodeTrace().forEachKeyValue((node, i) -> {
			var name = node.getName();
			if (name != null) {
				nodeNames.put(i, name);
			}
		});
		return nodeNames;
	}

	private static void appendError(PartialRelation symbol, StringBuilder errorsBuilder,
									Cursor<Tuple, TruthValue> cursor, IntObjectMap<String> nodeNames) {
		errorsBuilder.append('\t').append(symbol.name()).append("(");
		var key = cursor.getKey();
		int arity = key.getSize();
		for (int i = 0; i < arity; i++) {
			if (i > 0) {
				errorsBuilder.append(", ");
			}
			int nodeId = key.get(i);
			var name = nodeNames.get(nodeId);
			if (name == null) {
				errorsBuilder.append("::").append(i);
			} else {
				errorsBuilder.append(name);
			}
		}
		errorsBuilder.append("): error.\n");
	}
}
