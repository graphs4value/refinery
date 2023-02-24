package tools.refinery.store.query.substitution;

import tools.refinery.store.query.Variable;

import java.util.Map;

public final class Substitutions {
	private Substitutions() {
		throw new IllegalStateException("This is a static utility class and should not be instantiate directly");
	}

	public static Substitution total(Map<Variable, Variable> map) {
		return new MapBasedSubstitution(map, StatelessSubstitution.FAILING);
	}

	public static Substitution partial(Map<Variable, Variable> map) {
		return new MapBasedSubstitution(map, StatelessSubstitution.IDENTITY);
	}

	public static Substitution renewing(Map<Variable, Variable> map) {
		return new MapBasedSubstitution(map, renewing());
	}

	public static Substitution renewing() {
		return new RenewingSubstitution();
	}
}
