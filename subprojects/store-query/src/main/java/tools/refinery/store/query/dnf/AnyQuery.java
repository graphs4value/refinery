/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.query.dnf;

public sealed interface AnyQuery permits Query {
	String name();

	int arity();

	Class<?> valueType();

	Dnf getDnf();
}
