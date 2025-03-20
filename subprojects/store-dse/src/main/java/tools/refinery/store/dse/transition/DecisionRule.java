/*
 * SPDX-FileCopyrightText: 2025 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.dse.transition;

public record DecisionRule(Rule rule, int priority) {
	public static int DEFAULT_PRIORITY = 0;

	public DecisionRule(Rule rule) {
		this(rule, DEFAULT_PRIORITY);
	}
}
