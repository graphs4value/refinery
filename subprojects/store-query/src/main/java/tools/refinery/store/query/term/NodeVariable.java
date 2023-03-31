package tools.refinery.store.query.term;

import org.jetbrains.annotations.Nullable;
import tools.refinery.store.query.literal.ConstantLiteral;
import tools.refinery.store.query.literal.EquivalenceLiteral;

public final class NodeVariable extends Variable {
	NodeVariable(@Nullable String name) {
		super(name);
	}

	@Override
	public NodeSort getSort() {
		return NodeSort.INSTANCE;
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
	public NodeVariable asNodeVariable() {
		return this;
	}

	@Override
	public <T> DataVariable<T> asDataVariable(Class<T> type) {
		throw new IllegalStateException("%s is a node variable".formatted(this));
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
