/*
 * SPDX-FileCopyrightText: 2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.generator.tests;

import org.eclipse.core.runtime.AssertionFailedException;
import tools.refinery.generator.ModelSemantics;
import tools.refinery.generator.impl.FilteredInterpretation;
import tools.refinery.language.model.problem.Assertion;
import tools.refinery.language.model.problem.Node;
import tools.refinery.language.model.problem.NodeAssertionArgument;
import tools.refinery.language.model.problem.WildcardAssertionArgument;
import tools.refinery.language.semantics.ConstantParser;
import tools.refinery.logic.AbstractValue;
import tools.refinery.store.reasoning.ReasoningAdapter;
import tools.refinery.store.reasoning.interpretation.PartialInterpretation;
import tools.refinery.store.reasoning.literal.Concreteness;
import tools.refinery.store.reasoning.representation.PartialSymbol;
import tools.refinery.store.tuple.Tuple;

public record SemanticsExpectation(Assertion assertion, Concreteness concreteness, boolean exact,
                                   int lineNumber, String description, String source, Object expected) {
	public SemanticsExpectation(Assertion assertion, Concreteness concreteness, boolean exact,
	                            int lineNumber, String description, String source, ConstantParser parser) {
		this(assertion, concreteness, exact, lineNumber, description, source,
				// Extract the constant from the expression while it's still part of the EResource and we can run
				// type inference on it.
				parser.parseConstant(assertion.getValue()));
	}

	public void execute(ModelSemantics semantics) {
		var symbol = semantics.getProblemTrace().getPartialSymbol(assertion.getRelation());
		execute(semantics, (PartialSymbol<?, ?>) symbol);
	}

	private <A extends AbstractValue<A, C>, C> void execute(ModelSemantics semantics, PartialSymbol<A, C> symbol) {
		var reasoningAdapter = semantics.getModel().getAdapter(ReasoningAdapter.class);
		var interpretation = reasoningAdapter.getPartialInterpretation(concreteness, symbol);
		var existsInterpretation = reasoningAdapter.getPartialInterpretation(concreteness,
				ReasoningAdapter.EXISTS_SYMBOL);
		var filteredInterpretation = FilteredInterpretation.of(interpretation, existsInterpretation);

		var trace = semantics.getProblemTrace();
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

		var abstractType = symbol.abstractDomain().abstractType();
		var expectedValue = abstractType.cast(expected);
		if (wildcard) {
			checkWildcard(filteredInterpretation, nodeIds, expectedValue);
		} else {
			checkSingle(filteredInterpretation, nodeIds, expectedValue);
		}
	}

	private <A extends AbstractValue<A, C>, C> void checkSingle(
			PartialInterpretation<A, C> interpretation, int[] nodeIds, A expectedValue) {
		var tuple = Tuple.of(nodeIds);
		var actual = interpretation.get(tuple);
		if (assertionFailed(expectedValue, actual)) {
			throw new AssertionFailedException(getMessage(actual));
		}
	}

	private <A extends AbstractValue<A, C>, C> void checkWildcard(
			PartialInterpretation<A, C> interpretation, int[] nodeIds, A expectedValue) {
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

	private <A extends AbstractValue<A, C>, C> boolean assertionFailed(A expectedValue, A actual) {
		return !(exact ? actual.equals(expectedValue) : actual.isRefinementOf(expectedValue));
	}

	private String getMessage(Object actual) {
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
