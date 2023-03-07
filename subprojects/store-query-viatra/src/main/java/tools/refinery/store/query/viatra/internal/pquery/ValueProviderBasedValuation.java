package tools.refinery.store.query.viatra.internal.pquery;

import org.eclipse.viatra.query.runtime.matchers.psystem.IValueProvider;
import tools.refinery.store.query.term.DataVariable;
import tools.refinery.store.query.valuation.Valuation;

public record ValueProviderBasedValuation(IValueProvider valueProvider) implements Valuation {
	@Override
	public <T> T getValue(DataVariable<T> variable) {
		@SuppressWarnings("unchecked")
		var value = (T) valueProvider.getValue(variable.getUniqueName());
		return value;
	}
}
