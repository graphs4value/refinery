/*
 * SPDX-FileCopyrightText: 2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.generator.tests;

import org.eclipse.core.runtime.AssertionFailedException;
import tools.refinery.generator.ModelFacadeResult;
import tools.refinery.generator.ModelSemantics;
import tools.refinery.generator.ModelSemanticsFactory;
import tools.refinery.language.model.problem.Problem;
import tools.refinery.store.reasoning.literal.Concreteness;

import java.util.List;

public record SemanticsTestCase(String name, TestCaseKind kind, Problem problem,
                                List<SemanticsExpectation> expectations) {
	public void execute(ModelSemanticsFactory semanticsFactory) {
		semanticsFactory.withCandidateInterpretations(needsCandidateInterpretations());
		try (var semantics = semanticsFactory.tryCreateSemantics(problem)) {
			switch (kind) {
			case PROPAGATION_FAILURE -> {
				if (!semantics.getInitializationResult().isPropagationRejected()) {
					throw new AssertionFailedException("Expected propagation failure");
				}
			}
			case CONCRETIZATION_FAILURE -> {
				if (!semantics.getInitializationResult().isConcretizationRejected()) {
					throw new AssertionFailedException("Expected concretization failure");
				}
			}
			default -> {
				if (semantics.getInitializationResult() instanceof ModelFacadeResult.Rejected rejected) {
					throw new AssertionFailedException(rejected.formatMessage());
				}
			}
			}
			if (kind.noErrors()) {
				checkNoErrors(semantics);
			}
			for (var expectation : expectations) {
				expectation.execute(semantics);
			}
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
		var validationResult = semantics.checkConsistency();
		if (!validationResult.isConsistent()) {
			throw new AssertionFailedException(validationResult.formatMessage());
		}
	}
}
