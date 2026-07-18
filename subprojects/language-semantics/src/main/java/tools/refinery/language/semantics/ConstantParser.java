/*
 * SPDX-FileCopyrightText: 2026 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.semantics;

import com.google.inject.Inject;
import tools.refinery.language.expressions.ExprToTerm;
import tools.refinery.language.model.problem.Expr;
import tools.refinery.logic.term.AnyTerm;
import tools.refinery.logic.term.ConstantTerm;
import tools.refinery.logic.term.Term;

public class ConstantParser {
	@Inject
	private ExprToTerm exprToTerm;

	private <T> Term<T> parseTerm(Expr value, Class<T> expectedType) {
		if (value.eResource() == null) {
			throw new IllegalArgumentException("value must belong to an EResource");
		}
		return parseTerm(value).asType(expectedType);
	}

	private AnyTerm parseTerm(Expr value) {
		var term = (Term<?>) exprToTerm.toTerm(value)
				.orElseThrow(() -> new TracedException(value, "Invalid assertion value expression"));
		return term.reduce();
	}

	public <T> T parseConstant(Expr value, Class<T> expectedType) {
		var simplifiedTerm = parseTerm(value, expectedType);
		if (!(simplifiedTerm instanceof ConstantTerm<T> constantTerm)) {
			throw invalidConstantException(value);
		}
		return constantTerm.getValue();
	}

	public Object parseConstant(Expr value) {
		var simplifiedTerm = parseTerm(value);
		if (!(simplifiedTerm instanceof ConstantTerm<?> constantTerm)) {
			throw invalidConstantException(value);
		}
		return constantTerm.getValue();
	}

	private static RuntimeException invalidConstantException(Expr value) {
		return new TracedException(value, "Assertion value must be constant");
	}
}
