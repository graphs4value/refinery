package tools.refinery.store.query.viatra.internal.pquery;

import org.eclipse.viatra.query.runtime.matchers.psystem.IExpressionEvaluator;
import org.eclipse.viatra.query.runtime.matchers.psystem.IValueProvider;

import java.util.List;

public record CountNotEqualsExpressionEvaluator(boolean must, int threshold, String mayVariableName,
												String mustVariableName) implements IExpressionEvaluator {
	@Override
	public String getShortDescription() {
		return "%d %s not in [%s; %s]".formatted(threshold, must ? "must" : "may", mustVariableName, mayVariableName);
	}

	@Override
	public Iterable<String> getInputParameterNames() {
		return List.of(mayVariableName, mustVariableName);
	}

	@Override
	public Object evaluateExpression(IValueProvider provider) throws Exception {
		int mayCount = (Integer) provider.getValue(mayVariableName);
		int mustCount = (Integer) provider.getValue(mustVariableName);
		if (must) {
			return mayCount < threshold || mustCount > threshold;
		} else {
			return mayCount > threshold || mustCount < threshold;
		}
	}
}
