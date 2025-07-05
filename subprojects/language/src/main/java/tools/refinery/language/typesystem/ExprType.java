/*
 * SPDX-FileCopyrightText: 2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.typesystem;

public sealed interface ExprType permits FixedType, MutableType {
	NodeType NODE = new NodeType();
	LiteralType LITERAL = new LiteralType();
	InvalidType INVALID = new InvalidType();

	FixedType getActualType();

	ExprType unwrapIfSet();
}
