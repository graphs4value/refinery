package tools.refinery.store.query.dnf;

import tools.refinery.store.query.literal.CallLiteral;
import tools.refinery.store.query.literal.CallPolarity;
import tools.refinery.store.query.term.AssignedValue;
import tools.refinery.store.query.term.NodeSort;
import tools.refinery.store.query.term.NodeVariable;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

public final class RelationalQuery implements Query<Boolean> {
	private final Dnf dnf;

	RelationalQuery(Dnf dnf) {
		for (var parameter : dnf.getParameters()) {
			if (!(parameter instanceof NodeVariable)) {
				throw new IllegalArgumentException("Expected parameter %s of %s to be of sort %s, but got %s instead"
						.formatted(parameter, dnf, NodeSort.INSTANCE, parameter.getSort()));
			}
		}
		this.dnf = dnf;
	}

	@Override
	public String name() {
		return dnf.name();
	}

	@Override
	public int arity() {
		return dnf.arity();
	}

	@Override
	public Class<Boolean> valueType() {
		return Boolean.class;
	}

	@Override
	public Boolean defaultValue() {
		return false;
	}

	@Override
	public Dnf getDnf() {
		return dnf;
	}

	public CallLiteral call(CallPolarity polarity, List<NodeVariable> arguments) {
		return dnf.call(polarity, Collections.unmodifiableList(arguments));
	}

	public CallLiteral call(CallPolarity polarity, NodeVariable... arguments) {
		return dnf.call(polarity, arguments);
	}

	public CallLiteral call(NodeVariable... arguments) {
		return dnf.call(arguments);
	}

	public CallLiteral callTransitive(NodeVariable left, NodeVariable right) {
		return dnf.callTransitive(left, right);
	}

	public AssignedValue<Integer> count(List<NodeVariable> arguments) {
		return dnf.count(Collections.unmodifiableList(arguments));
	}

	public AssignedValue<Integer> count(NodeVariable... arguments) {
		return dnf.count(arguments);
	}


	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		RelationalQuery that = (RelationalQuery) o;
		return dnf.equals(that.dnf);
	}

	@Override
	public int hashCode() {
		return Objects.hash(dnf);
	}

	@Override
	public String toString() {
		return dnf.toString();
	}
}
