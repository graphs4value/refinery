/*
 * SPDX-FileCopyrightText: 2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.typesystem;

public final class ModalLiteralType implements FixedType {
	ModalLiteralType() {
	}

	@Override
	public String toString() {
		return "modal constraint";
	}
}
