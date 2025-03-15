/*
 * SPDX-FileCopyrightText: 2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.generator.tests;

import org.eclipse.core.runtime.AssertionFailedException;
import tools.refinery.generator.ModelSemantics;
import tools.refinery.generator.ModelSemanticsFactory;
import tools.refinery.language.model.problem.Problem;
import tools.refinery.store.reasoning.literal.Concreteness;

import java.util.List;

public record SemanticsTestCase(String name, boolean allowErrors, Problem problem,
								List<SemanticsExpectation> expectations) {
	public void execute(ModelSemanticsFactory semanticsFactory) {
		semanticsFactory.withCandidateInterpretations(needsCandidateInterpretations());
		try (var semantics = semanticsFactory.createSemantics(problem)) {
			if (!allowErrors) {
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
