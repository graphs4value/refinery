/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.query.term;

public enum ArithmeticUnaryOperator {
	PLUS("+"),
	MINUS("-");

	private final String prefix;

	ArithmeticUnaryOperator(String prefix) {
		this.prefix = prefix;
	}

	public String formatString(String body) {
		return "%s(%s)".formatted(prefix, body);
	}
}
