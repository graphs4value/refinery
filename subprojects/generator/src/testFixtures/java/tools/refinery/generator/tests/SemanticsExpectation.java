/*
 * SPDX-FileCopyrightText: 2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.generator.tests;

import org.eclipse.core.runtime.AssertionFailedException;
import tools.refinery.generator.impl.FilteredInterpretation;
import tools.refinery.generator.ModelSemantics;
import tools.refinery.language.model.problem.Assertion;
import tools.refinery.language.model.problem.Node;
import tools.refinery.language.model.problem.NodeAssertionArgument;
import tools.refinery.language.model.problem.WildcardAssertionArgument;
import tools.refinery.language.semantics.SemanticsUtils;
import tools.refinery.logic.term.truthvalue.TruthValue;
import tools.refinery.store.reasoning.ReasoningAdapter;
import tools.refinery.store.reasoning.interpretation.PartialInterpretation;
import tools.refinery.store.reasoning.literal.Concreteness;
import tools.refinery.store.tuple.Tuple;

public record SemanticsExpectation(Assertion assertion, Concreteness concreteness, boolean exact,
								   int lineNumber, String description, String source) {
	public void execute(ModelSemantics semantics) {
		var trace = semantics.getProblemTrace();
		var symbol = trace.getPartialRelation(assertion.getRelation());
		var reasoningAdapter = semantics.getModel().getAdapter(ReasoningAdapter.class);
		var interpretation = reasoningAdapter.getPartialInterpretation(concreteness, symbol);
		var existsInterpretation = reasoningAdapter.getPartialInterpretation(concreteness,
				ReasoningAdapter.EXISTS_SYMBOL);
		var filteredInterpretation = new FilteredInterpretation<>(interpretation, existsInterpretation);

		var arguments = assertion.getArguments();
		int arity = arguments.size();
		var nodeIds = new int[arity];
		boolean wildcard = false;
		for (int i = 0; i < arity; i++) {
			var argument = arguments.get(i);
			switch (argument) {
			case NodeAssertionArgument nodeAssertionArgument -> {
				var nodeOrVariable = nodeAssertionArgument.getNode();
				if (!(nodeOrVariable instanceof Node node)) {
					throw new IllegalArgumentException("Invalid Node: " + nodeOrVariable);
				}
				nodeIds[i] = trace.getNodeId(node);
			}
			case WildcardAssertionArgument ignored -> {
				nodeIds[i] = 1;
				wildcard = true;
			}
			default -> throw new IllegalArgumentException("Invalid AssertionArgument: " + argument);
			}
		}

		var expectedValue = SemanticsUtils.getTruthValue(assertion.getValue());
		if (wildcard) {
			checkWildcard(filteredInterpretation, nodeIds, expectedValue);
		} else {
			checkSingle(filteredInterpretation, nodeIds, expectedValue);
		}
	}


	private void checkSingle(PartialInterpretation<TruthValue, Boolean> interpretation, int[] nodeIds,
							 TruthValue expectedValue) {
		var tuple = Tuple.of(nodeIds);
		var actual = interpretation.get(tuple);
		if (assertionFailed(expectedValue, actual)) {
			throw new AssertionFailedException(getMessage(actual));
		}
	}

	private void checkWildcard(PartialInterpretation<TruthValue, Boolean> interpretation, int[] nodeIds,
							   TruthValue expectedValue) {
		int arity = nodeIds.length;
		var cursor = interpretation.getAll();
		while (cursor.move()) {
			var key = cursor.getKey();
			boolean matches = true;
			for (int i = 0; matches && i < arity; i++) {
				int nodeId = nodeIds[i];
				if (nodeId >= 0 && key.get(i) != nodeId) {
					matches = false;
				}
			}
			if (matches && assertionFailed(expectedValue, cursor.getValue())) {
				throw new AssertionFailedException(getMessage(null));
			}
		}
	}

	private boolean assertionFailed(TruthValue expectedValue, TruthValue actual) {
		return !(exact ? actual.equals(expectedValue) : actual.isRefinementOf(expectedValue));
	}

	private String getMessage(TruthValue actual) {
		var messageBuilder = new StringBuilder();
		messageBuilder.append("EXPECT");
		if (concreteness == Concreteness.CANDIDATE) {
			messageBuilder.append(" CANDIDATE");
		}
		if (exact) {
			messageBuilder.append(" EXACTLY");
		}
		messageBuilder.append(" ").append(source);
		if (description != null) {
			messageBuilder.append(" (").append(description).append(")");
		}
		if (actual == null) {
			messageBuilder.append(" failed");
		} else {
			messageBuilder.append(" was ").append(actual).append(" instead");
		}
		messageBuilder.append(" in line ").append(lineNumber);
		return messageBuilder.toString();
	}
}
