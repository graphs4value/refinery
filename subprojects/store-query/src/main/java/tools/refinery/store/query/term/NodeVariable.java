/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.query.term;

import org.jetbrains.annotations.Nullable;
import tools.refinery.store.query.InvalidQueryException;
import tools.refinery.store.query.literal.ConstantLiteral;
import tools.refinery.store.query.literal.EquivalenceLiteral;

import java.util.Optional;

public final class NodeVariable extends Variable {
	NodeVariable(@Nullable String name) {
		super(name);
	}

	@Override
	public Optional<Class<?>> tryGetType() {
		return Optional.empty();
	}

	@Override
	public NodeVariable renew(@Nullable String name) {
		return Variable.of(name);
	}

	@Override
	public NodeVariable renew() {
		return renew(getExplicitName());
	}

	@Override
	public boolean isNodeVariable() {
		return true;
	}

	@Override
	public boolean isDataVariable() {
		return false;
	}

	@Override
	public NodeVariable asNodeVariable() {
		return this;
	}

	@Override
	public <T> DataVariable<T> asDataVariable(Class<T> type) {
		throw new InvalidQueryException("%s is a node variable".formatted(this));
	}

	@Override
	public int hashCodeWithSubstitution(int sequenceNumber) {
		return sequenceNumber;
	}

	public ConstantLiteral isConstant(int value) {
		return new ConstantLiteral(this, value);
	}

	public EquivalenceLiteral isEquivalent(NodeVariable other) {
		return new EquivalenceLiteral(true, this, other);
	}

	public EquivalenceLiteral notEquivalent(NodeVariable other) {
		return new EquivalenceLiteral(false, this, other);
	}
}
