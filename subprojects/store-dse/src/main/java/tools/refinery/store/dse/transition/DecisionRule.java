/*
 * SPDX-FileCopyrightText: 2025 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.dse.transition;

public record DecisionRule(Rule rule, int priority, double coefficient, double exponent) {
	public static final int DEFAULT_PRIORITY = 0;
	public static final double DEFAULT_COEFFICIENT = 1;
	public static final double DEFAULT_EXPONENT = 1;

	public DecisionRule(Rule rule) {
		this(rule, DEFAULT_PRIORITY);
	}

	public DecisionRule(Rule rule, int priority) {
		this(rule, priority, DEFAULT_COEFFICIENT, DEFAULT_EXPONENT);
	}

	public DecisionRule {
		if (coefficient <= 0) {
			throw new IllegalArgumentException("Decision rule weight coefficient must be positive");
		}
		if (exponent < 0) {
			throw new IllegalArgumentException("Decision rule weight exponent must not be negative");
		}
	}

	public double getWeight(int numberOfUnvisitedActivations) {
		if (numberOfUnvisitedActivations == 0) {
			return 0;
		}
		return coefficient * Math.pow(numberOfUnvisitedActivations, exponent);
	}
}
