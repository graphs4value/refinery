package tools.refinery.store.query.term;

import org.jetbrains.annotations.Nullable;

public final class NodeSort implements Sort {
	public static final NodeSort INSTANCE = new NodeSort();

	private NodeSort() {
	}

	@Override
	public boolean isInstance(Variable variable) {
		return variable instanceof NodeVariable;
	}

	@Override
	public NodeVariable newInstance(@Nullable String name) {
		return new NodeVariable(name);
	}

	@Override
	public NodeVariable newInstance() {
		return newInstance(null);
	}

	@Override
	public String toString() {
		return "<node>";
	}
}
