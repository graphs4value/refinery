/*
 * SPDX-FileCopyrightText: 2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.typesystem;

public final class MutableType implements ExprType {
	private DataExprType actualType;

	@Override
	public FixedType getActualType() {
		return actualType == null ? INVALID : actualType;
	}

	public void setActualType(DataExprType actualType) {
		if (this.actualType != null) {
			throw new IllegalStateException("Actual type was already set");
		}
		this.actualType = actualType;
	}

	@Override
	public ExprType unwrapIfSet() {
		return actualType == null ? this : actualType;
	}

	@Override
	public String toString() {
		return getActualType().toString();
	}
}
