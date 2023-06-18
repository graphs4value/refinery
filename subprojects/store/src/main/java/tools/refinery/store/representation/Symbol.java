/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.representation;

public record Symbol<T>(String name, int arity, Class<T> valueType, T defaultValue) implements AnySymbol {
	@Override
	public String toString() {
		return "%s/%d".formatted(name, arity);
	}

	public static Symbol<Boolean> of(String name, int arity) {
		return of(name, arity, Boolean.class, false);
	}

	public static <T> Symbol<T> of(String name, int arity, Class<T> valueType) {
		return of(name, arity, valueType, null);
	}

	public static <T> Symbol<T> of(String name, int arity, Class<T> valueType, T defaultValue) {
		return new Symbol<>(name, arity, valueType, defaultValue);
	}
}
