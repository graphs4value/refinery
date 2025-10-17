/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.representation;

import org.jetbrains.annotations.NotNull;

public record Symbol<T>(String name, int arity, Class<T> valueType, T defaultValue) implements AnySymbol {
	@Override
	public boolean equals(Object o) {
		return this == o;
	}

	@Override
	public int hashCode() {
		// Compare by identity to make hash table look-ups more efficient.
		return System.identityHashCode(this);
	}

	@Override
	public @NotNull String toString() {
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
