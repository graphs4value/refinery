package tools.refinery.store.query.term;

import org.jetbrains.annotations.Nullable;
import tools.refinery.store.query.dnf.DnfUtils;

import java.util.Objects;

public abstract sealed class Variable permits AnyDataVariable, NodeVariable {
	private final String explicitName;
	private final String uniqueName;

	protected Variable(String name) {
		this.explicitName = name;
		uniqueName = DnfUtils.generateUniqueName(name);
	}

	public abstract Sort getSort();

	public String getName() {
		return explicitName == null ? uniqueName : explicitName;
	}

	protected String getExplicitName() {
		return explicitName;
	}

	public boolean isExplicitlyNamed() {
		return explicitName != null;
	}

	public String getUniqueName() {
		return uniqueName;
	}

	public abstract Variable renew(@Nullable String name);

	public abstract Variable renew();

	public abstract NodeVariable asNodeVariable();

	public abstract <T> DataVariable<T> asDataVariable(Class<T> type);

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

	public static NodeVariable of(@Nullable String name) {
		return new NodeVariable(name);
	}

	public static NodeVariable of() {
		return of((String) null);
	}

	public static <T> DataVariable<T> of(@Nullable String name, Class<T> type) {
		return new DataVariable<>(name, type);
	}

	public static <T> DataVariable<T> of(Class<T> type) {
		return of(null, type);
	}
}
