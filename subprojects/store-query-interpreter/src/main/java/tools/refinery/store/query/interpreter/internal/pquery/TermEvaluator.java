/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.query.interpreter.internal.pquery;

import tools.refinery.store.query.term.Term;
import tools.refinery.store.query.term.Variable;
import tools.refinery.interpreter.matchers.psystem.IExpressionEvaluator;
import tools.refinery.interpreter.matchers.psystem.IValueProvider;

import java.util.stream.Collectors;

class TermEvaluator<T> implements IExpressionEvaluator {
	private final Term<T> term;

	public TermEvaluator(Term<T> term) {
		this.term = term;
	}

	@Override
	public String getShortDescription() {
		return term.toString();
	}

	@Override
	public Iterable<String> getInputParameterNames() {
		return term.getInputVariables().stream().map(Variable::getUniqueName).collect(Collectors.toUnmodifiableSet());
	}

	@Override
	public Object evaluateExpression(IValueProvider provider) {
		var valuation = new ValueProviderBasedValuation(provider);
		return term.evaluate(valuation);
	}
}
