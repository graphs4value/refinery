/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.tuple;

final class TupleConstants {
	public static final int MAX_STATIC_ARITY_TUPLE_SIZE = 4;
	public static final String TUPLE_BEGIN = "[";
	public static final String TUPLE_SEPARATOR = ", ";
	public static final String TUPLE_END = "]";

	private TupleConstants() {
		throw new IllegalArgumentException("This is a static utility class an should not instantiated directly");
	}
}
