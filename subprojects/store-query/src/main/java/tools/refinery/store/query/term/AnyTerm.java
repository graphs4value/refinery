package tools.refinery.store.query.term;

import tools.refinery.store.query.equality.LiteralEqualityHelper;
import tools.refinery.store.query.substitution.Substitution;

import java.util.Set;

public sealed interface AnyTerm permits AnyDataVariable, Term {
	Class<?> getType();

	AnyTerm substitute(Substitution substitution);

	boolean equalsWithSubstitution(LiteralEqualityHelper helper, AnyTerm other);

	Set<AnyDataVariable> getInputVariables();
}
