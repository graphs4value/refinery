/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.semantics.internal;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import tools.refinery.logic.AbstractValue;
import tools.refinery.logic.term.truthvalue.TruthValue;

public sealed interface DecisionTreeValue<A extends AbstractValue<A, C>, C> {
	DecisionTreeValue<?, ?> UNSET = new Unset<TruthValue, Boolean>();

	boolean isUnset();

	@Nullable A orElseNull();

	A merge(@Nullable A other);

	DecisionTreeValue<A, C> overwrite(DecisionTreeValue<A, C> other);

	A orElse(A other);

	static <A extends AbstractValue<A, C>, C> DecisionTreeValue<A, C> unset() {
		// This is safe, because {@code Unset} doesn't hold any reference to an A or C instance.
		@SuppressWarnings("unchecked")
		var typedUnset = (DecisionTreeValue<A, C>) UNSET;
		return typedUnset;
	}

	static <A extends AbstractValue<A, C>, C> DecisionTreeValue<A, C> ofNullable(@Nullable A value) {
		return value == null ? unset() : new Some<>(value);
	}

	record Some<A extends AbstractValue<A, C>, C>(@NotNull A value) implements DecisionTreeValue<A, C> {
		public Some {
			// We add a runtime check for the static analysis annotation.
			//noinspection ConstantValue
			if (value == null) {
				throw new IllegalArgumentException("value must not be null");
			}
		}

		@Override
		public boolean isUnset() {
			return false;
		}

		@Override
		public @NotNull A orElseNull() {
			return value;
		}

		@Override
		public A merge(@Nullable A other) {
			return other == null ? value : value.meet(other);
		}

		@Override
		public DecisionTreeValue<A, C> overwrite(DecisionTreeValue<A, C> other) {
			return other.isUnset() ? this : other;
		}

		@Override
		public A orElse(A other) {
			return value;
		}
	}

	final class Unset<A extends AbstractValue<A, C>, C> implements DecisionTreeValue<A, C> {
		@Override
		public boolean isUnset() {
			return true;
		}

		@Override
		public @Nullable A orElseNull() {
			return null;
		}

		@Override
		public A merge(@Nullable A other) {
			return other;
		}

		@Override
		public DecisionTreeValue<A, C> overwrite(DecisionTreeValue<A, C> other) {
			return other;
		}

		@Override
		public A orElse(A other) {
			return other;
		}
	}
}
