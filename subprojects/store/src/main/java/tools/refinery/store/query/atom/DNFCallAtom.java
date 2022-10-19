package tools.refinery.store.query.atom;

import tools.refinery.store.query.DNF;
import tools.refinery.store.query.Variable;

import java.util.List;
import java.util.Objects;

public final class DNFCallAtom extends AbstractCallAtom<DNF> {
	public DNFCallAtom(CallKind kind, DNF target, List<Variable> substitution) {
		super(kind, target, substitution);
	}

	public DNFCallAtom(CallKind kind, DNF target, Variable... substitution) {
		super(kind, target, List.of(substitution));
	}

	public DNFCallAtom(DNF target, List<Variable> substitution) {
		this(CallKind.POSITIVE, target, substitution);
	}

	public DNFCallAtom(DNF target, Variable... substitution) {
		this(target, List.of(substitution));
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		DNFCallAtom dnfCallAtom = (DNFCallAtom) o;
		return Objects.equals(getKind(), dnfCallAtom.getKind())
				&& Objects.equals(getTarget(), dnfCallAtom.getTarget())
				&& Objects.equals(getSubstitution(), dnfCallAtom.getSubstitution());
	}

	@Override
	public int hashCode() {
		return Objects.hash(getKind(), getTarget(), getSubstitution());
	}
}
