package tools.refinery.store.query.viatra.internal.pquery;

import org.eclipse.viatra.query.runtime.matchers.psystem.IValueProvider;
import tools.refinery.store.query.term.Term;

class AssumptionEvaluator extends TermEvaluator<Boolean> {
	public AssumptionEvaluator(Term<Boolean> term) {
		super(term);
	}

	@Override
	public Object evaluateExpression(IValueProvider provider) {
		var result = super.evaluateExpression(provider);
		return result == null ? Boolean.FALSE : result;
	}
}
