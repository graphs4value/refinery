package tools.refinery.store.query.literal;

import tools.refinery.store.query.equality.LiteralEqualityHelper;
import tools.refinery.store.query.substitution.Substitution;
import tools.refinery.store.query.term.Term;
import tools.refinery.store.query.term.Variable;
import tools.refinery.store.query.term.bool.BoolConstantTerm;

import java.util.Set;

public record AssumeLiteral(Term<Boolean> term) implements Literal {
	public AssumeLiteral {
		if (!term.getType().equals(Boolean.class)) {
			throw new IllegalArgumentException("Term %s must be of type %s, got %s instead".formatted(
					term, Boolean.class.getName(), term.getType().getName()));
		}
	}

	@Override
	public Set<Variable> getBoundVariables() {
		return Set.of();
	}

	@Override
	public Literal substitute(Substitution substitution) {
		return new AssumeLiteral(term.substitute(substitution));
	}

	@Override
	public boolean equalsWithSubstitution(LiteralEqualityHelper helper, Literal other) {
		if (other == null || getClass() != other.getClass()) {
			return false;
		}
		var otherAssumeLiteral = (AssumeLiteral) other;
		return term.equalsWithSubstitution(helper, otherAssumeLiteral.term);
	}

	@Override
	public LiteralReduction getReduction() {
		if (BoolConstantTerm.TRUE.equals(term)) {
			return LiteralReduction.ALWAYS_TRUE;
		} else if (BoolConstantTerm.FALSE.equals(term)) {
			return LiteralReduction.ALWAYS_FALSE;
		} else {
			return LiteralReduction.NOT_REDUCIBLE;
		}
	}

	@Override
	public String toString() {
		return "(%s)".formatted(term);
	}
}
