/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.representation;

import java.util.Optional;

// Singleton pattern, because there is only one domain for truth values.
@SuppressWarnings("squid:S6548")
public final class TruthValueDomain implements AbstractDomain<TruthValue, Boolean> {
	public static final TruthValueDomain INSTANCE = new TruthValueDomain();

	private TruthValueDomain() {
	}

	@Override
	public Class<TruthValue> abstractType() {
		return TruthValue.class;
	}

	@Override
	public Class<Boolean> concreteType() {
		return Boolean.class;
	}

	@Override
	public TruthValue toAbstract(Boolean concreteValue) {
		return TruthValue.toTruthValue(concreteValue);
	}

	@Override
	public Optional<Boolean> toConcrete(TruthValue abstractValue) {
		return switch (abstractValue) {
			case TRUE -> Optional.of(true);
			case FALSE -> Optional.of(false);
			default -> Optional.empty();
		};
	}

	@Override
	public boolean isConcrete(TruthValue abstractValue) {
		return abstractValue.isConcrete();
	}

	@Override
	public TruthValue commonRefinement(TruthValue leftValue, TruthValue rightValue) {
		return leftValue.merge(rightValue);
	}

	@Override
	public TruthValue commonAncestor(TruthValue leftValue, TruthValue rightValue) {
		return leftValue.join(rightValue);
	}

	@Override
	public TruthValue unknown() {
		return TruthValue.UNKNOWN;
	}

	@Override
	public boolean isError(TruthValue abstractValue) {
		return !abstractValue.isConsistent();
	}
}
