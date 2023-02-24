package tools.refinery.store.query;

import tools.refinery.store.query.literal.ConstantLiteral;
import tools.refinery.store.query.literal.EquivalenceLiteral;

import java.util.Objects;

public class Variable {
	private final String name;
	private final String uniqueName;

	public Variable() {
		this(null);
	}

	public Variable(String name) {
		super();
		this.name = name;
		this.uniqueName = DnfUtils.generateUniqueName(name);

	}
	public String getName() {
		return name == null ? uniqueName : name;
	}

	public boolean isExplicitlyNamed() {
		return name != null;
	}

	public String getUniqueName() {
		return uniqueName;
	}

	public ConstantLiteral isConstant(int value) {
		return new ConstantLiteral(this, value);
	}

	public EquivalenceLiteral isEquivalent(Variable other) {
		return new EquivalenceLiteral(true, this, other);
	}

	public EquivalenceLiteral notEquivalent(Variable other) {
		return new EquivalenceLiteral(false, this, other);
	}

	@Override
	public String toString() {
		return getName();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Variable variable = (Variable) o;
		return Objects.equals(uniqueName, variable.uniqueName);
	}

	@Override
	public int hashCode() {
		return Objects.hash(uniqueName);
	}
}
