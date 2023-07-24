/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.representation;

public sealed interface AnySymbol permits Symbol {
	String name();

	int arity();

	Class<?> valueType();
}
