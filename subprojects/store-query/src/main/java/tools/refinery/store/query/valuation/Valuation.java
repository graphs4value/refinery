package tools.refinery.store.query.valuation;

import org.jetbrains.annotations.Nullable;
import tools.refinery.store.query.substitution.Substitution;
import tools.refinery.store.query.term.AnyDataVariable;
import tools.refinery.store.query.term.DataVariable;

import java.util.Set;

public interface Valuation {
	<T> T getValue(DataVariable<T> variable);

	default Valuation substitute(@Nullable Substitution substitution) {
		if (substitution == null) {
			return this;
		}
		return new SubstitutedValuation(this, substitution);
	}

	default Valuation restrict(Set<? extends AnyDataVariable> allowedVariables) {
		return new RestrictedValuation(this, Set.copyOf(allowedVariables));
	}
}
