/*
 * SPDX-FileCopyrightText: 2025 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.utils;

public record DecisionSettings(int priority, double coefficient, double exponent) {
	public static final int DEFAULT_PRIORITY = 0;
	public static final double DEFAULT_COEFFICIENT = 1;
	public static final double DEFAULT_EXPONENT = 1;

	public DecisionSettings(int priority) {
		this(priority, DEFAULT_COEFFICIENT, DEFAULT_EXPONENT);
	}
}
