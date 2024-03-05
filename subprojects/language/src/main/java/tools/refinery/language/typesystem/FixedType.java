/*
 * SPDX-FileCopyrightText: 2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.typesystem;

public sealed interface FixedType extends ExprType permits NodeType, LiteralType, InvalidType, DataExprType {
	@Override
	default FixedType getActualType() {
		return this;
	}

	@Override
	default FixedType unwrapIfSet() {
		return this;
	}
}
