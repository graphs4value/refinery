/*
 * SPDX-FileCopyrightText: 2025 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.utils;

import java.math.BigDecimal;

public record DecisionSettings(int priority, BigDecimal coefficient, BigDecimal exponent) {
	public static final int DEFAULT_PRIORITY = 0;
	public static final BigDecimal DEFAULT_COEFFICIENT = BigDecimal.ONE;
	public static final BigDecimal DEFAULT_EXPONENT = BigDecimal.ONE;

	public DecisionSettings(int priority) {
		this(priority, DEFAULT_COEFFICIENT, DEFAULT_EXPONENT);
	}
}
