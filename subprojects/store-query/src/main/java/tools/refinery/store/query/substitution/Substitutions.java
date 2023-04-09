/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.query.substitution;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import tools.refinery.store.query.term.Variable;

import java.util.Map;

public final class Substitutions {
	private Substitutions() {
		throw new IllegalStateException("This is a static utility class and should not be instantiate directly");
	}

	public static Substitution total(Map<Variable, Variable> map) {
		return new MapBasedSubstitution(map, StatelessSubstitution.FAILING);
	}

	public static Substitution partial(Map<Variable, Variable> map) {
		return new MapBasedSubstitution(map, StatelessSubstitution.IDENTITY);
	}

	public static Substitution renewing(Map<Variable, Variable> map) {
		return new MapBasedSubstitution(map, renewing());
	}

	public static Substitution renewing() {
		return new RenewingSubstitution();
	}

	public static Substitution compose(@Nullable Substitution first, @NotNull Substitution second) {
		return first == null ? second : first.andThen(second);
	}
}
