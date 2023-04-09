/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.query.term;

public enum ArithmeticBinaryOperator {
	ADD("+", true),
	SUB("-", true),
	MUL("*", true),
	DIV("/", true),
	POW("**", true),
	MIN("min", false),
	MAX("max", false);

	private final String text;
	private final boolean infix;

	ArithmeticBinaryOperator(String text, boolean infix) {
		this.text = text;
		this.infix = infix;
	}

	public String formatString(String left, String right) {
		if (infix) {
			return "(%s) %s (%s)".formatted(left, text, right);
		}
		return "%s(%s, %s)".formatted(text, left, right);
	}
}
