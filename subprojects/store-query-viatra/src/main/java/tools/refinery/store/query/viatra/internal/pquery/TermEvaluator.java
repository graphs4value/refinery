package tools.refinery.store.query.viatra.internal.pquery;

import org.eclipse.viatra.query.runtime.matchers.psystem.IExpressionEvaluator;
import org.eclipse.viatra.query.runtime.matchers.psystem.IValueProvider;
import tools.refinery.store.query.term.Term;
import tools.refinery.store.query.term.Variable;

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
