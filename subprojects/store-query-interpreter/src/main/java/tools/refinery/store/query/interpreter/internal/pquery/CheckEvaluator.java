/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.query.interpreter.internal.pquery;

import tools.refinery.interpreter.matchers.psystem.IValueProvider;
import tools.refinery.store.query.term.Term;

class CheckEvaluator extends TermEvaluator<Boolean> {
	public CheckEvaluator(Term<Boolean> term) {
		super(term);
	}

	@Override
	public Object evaluateExpression(IValueProvider provider) {
		var result = super.evaluateExpression(provider);
		return result == null ? Boolean.FALSE : result;
	}
}
