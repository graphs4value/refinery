package tools.refinery.store.query.viatra.internal.pquery;

import org.eclipse.viatra.query.runtime.matchers.psystem.IExpressionEvaluator;
import org.eclipse.viatra.query.runtime.matchers.psystem.IValueProvider;
import tools.refinery.store.query.atom.ComparisonOperator;
import tools.refinery.store.query.atom.CountingPolarity;

import java.util.List;

public record CountExpressionEvaluator(String variableName, ComparisonOperator operator,
									   int threshold) implements IExpressionEvaluator {
	public CountExpressionEvaluator(String variableName, CountingPolarity polarity) {
		this(variableName, polarity.operator(), polarity.threshold());
	}

	@Override
	public String getShortDescription() {
		return "%s %s %d".formatted(variableName, operator, threshold);
	}

	@Override
	public Iterable<String> getInputParameterNames() {
		return List.of(variableName);
	}

	@Override
	public Object evaluateExpression(IValueProvider provider) {
		int value = (Integer) provider.getValue(variableName);
		return switch (operator) {
			case EQUALS -> value == threshold;
			case NOT_EQUALS -> value != threshold;
			case LESS -> value < threshold;
			case LESS_EQUALS -> value <= threshold;
			case GREATER -> value > threshold;
			case GREATER_EQUALS -> value >= threshold;
		};
	}
}
