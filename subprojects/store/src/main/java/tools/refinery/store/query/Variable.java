package tools.refinery.store.query;

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
		this.uniqueName = DNFUtils.generateUniqueName(name);

	}
	public String getName() {
		return name;
	}

	public String getUniqueName() {
		return uniqueName;
	}

	public boolean isNamed() {
		return name != null;
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
