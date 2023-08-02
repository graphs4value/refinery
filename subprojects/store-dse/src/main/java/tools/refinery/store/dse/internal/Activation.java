package tools.refinery.store.dse.internal;

import tools.refinery.store.tuple.Tuple;

public record Activation(TransformationRule transformationRule, Tuple activation) {
	public boolean fire() {
		return transformationRule.fireActivation(activation);
	}
}
