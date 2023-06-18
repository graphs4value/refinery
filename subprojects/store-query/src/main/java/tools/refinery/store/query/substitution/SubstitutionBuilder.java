/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.query.substitution;

import tools.refinery.store.query.term.DataVariable;
import tools.refinery.store.query.term.NodeVariable;
import tools.refinery.store.query.term.Variable;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SuppressWarnings("UnusedReturnValue")
public class SubstitutionBuilder {
	private final Map<Variable, Variable> map = new HashMap<>();
	private Substitution fallback;

	SubstitutionBuilder() {
		total();
	}

	public SubstitutionBuilder put(NodeVariable original, NodeVariable substitute) {
		return putChecked(original, substitute);
	}

	public <T> SubstitutionBuilder put(DataVariable<T> original, DataVariable<T> substitute) {
		return putChecked(original, substitute);
	}

	public SubstitutionBuilder putChecked(Variable original, Variable substitute) {
		if (!original.tryGetType().equals(substitute.tryGetType())) {
			throw new IllegalArgumentException("Cannot substitute variable %s of sort %s with variable %s of sort %s"
					.formatted(original, original.tryGetType().map(Class::getName).orElse("node"), substitute,
							substitute.tryGetType().map(Class::getName).orElse("node")));
		}
		if (map.containsKey(original)) {
			throw new IllegalArgumentException("Already has substitution for variable %s".formatted(original));
		}
		map.put(original, substitute);
		return this;
	}

	public SubstitutionBuilder putManyChecked(List<Variable> originals, List<Variable> substitutes) {
		int size = originals.size();
		if (size != substitutes.size()) {
			throw new IllegalArgumentException("Cannot substitute %d variables %s with %d variables %s"
					.formatted(size, originals, substitutes.size(), substitutes));
		}
		for (int i = 0; i < size; i++) {
			putChecked(originals.get(i), substitutes.get(i));
		}
		return this;
	}

	public SubstitutionBuilder fallback(Substitution newFallback) {
		fallback = newFallback;
		return this;
	}

	public SubstitutionBuilder total() {
		return fallback(StatelessSubstitution.FAILING);
	}

	public SubstitutionBuilder partial() {
		return fallback(StatelessSubstitution.IDENTITY);
	}

	public SubstitutionBuilder renewing() {
		return fallback(new RenewingSubstitution());
	}

	public Substitution build() {
		return map.isEmpty() ? fallback : new MapBasedSubstitution(Collections.unmodifiableMap(map), fallback);
	}
}
