/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.representation;

import java.util.Optional;

public final class TruthValueDomain implements AbstractDomain<TruthValue, Boolean> {
	public static final TruthValueDomain INSTANCE = new TruthValueDomain();

	private TruthValueDomain() {
	}

	@Override
	public Class<TruthValue> abstractType() {
		return null;
	}

	@Override
	public Class<Boolean> concreteType() {
		return null;
	}

	@Override
	public TruthValue toAbstract(Boolean concreteValue) {
		return null;
	}

	@Override
	public Optional<Boolean> toConcrete(TruthValue abstractValue) {
		return Optional.empty();
	}

	@Override
	public boolean isConcrete(TruthValue abstractValue) {
		return AbstractDomain.super.isConcrete(abstractValue);
	}

	@Override
	public boolean isRefinement(TruthValue originalValue, TruthValue refinedValue) {
		return false;
	}

	@Override
	public TruthValue commonRefinement(TruthValue leftValue, TruthValue rightValue) {
		return null;
	}

	@Override
	public TruthValue commonAncestor(TruthValue leftValue, TruthValue rightValue) {
		return null;
	}

	@Override
	public TruthValue unknown() {
		return null;
	}

	@Override
	public boolean isError(TruthValue abstractValue) {
		return false;
	}
}
