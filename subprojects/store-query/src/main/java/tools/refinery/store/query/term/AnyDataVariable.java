/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.query.term;

import org.jetbrains.annotations.Nullable;
import tools.refinery.store.query.InvalidQueryException;
import tools.refinery.store.query.equality.LiteralEqualityHelper;

import java.util.Optional;
import java.util.Set;

public abstract sealed class AnyDataVariable extends Variable implements AnyTerm permits DataVariable {
	protected AnyDataVariable(String name) {
		super(name);
	}

	@Override
	public Optional<Class<?>> tryGetType() {
		return Optional.of(getType());
	}

	@Override
	public boolean isNodeVariable() {
		return false;
	}

	@Override
	public boolean isDataVariable() {
		return true;
	}

	@Override
	public NodeVariable asNodeVariable() {
		throw new InvalidQueryException("%s is a data variable".formatted(this));
	}

	@Override
	public boolean equalsWithSubstitution(LiteralEqualityHelper helper, AnyTerm other) {
		return other instanceof AnyDataVariable dataVariable && helper.variableEqual(this, dataVariable);
	}

	@Override
	public Set<AnyDataVariable> getInputVariables() {
		return Set.of(this);
	}

	@Override
	public abstract AnyDataVariable renew(@Nullable String name);

	@Override
	public abstract AnyDataVariable renew();
}
