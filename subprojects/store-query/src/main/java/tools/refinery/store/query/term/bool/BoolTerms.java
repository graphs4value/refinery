package tools.refinery.store.query.term.bool;

import tools.refinery.store.query.term.ConstantTerm;
import tools.refinery.store.query.term.Term;

public final class BoolTerms {
	private BoolTerms() {
		throw new IllegalArgumentException("This is a static utility class and should not be instantiated directly");
	}

	public static ConstantTerm<Boolean> constant(boolean value) {
		return BoolConstantTerm.valueOf(value);
	}

	public static BoolNotTerm not(Term<Boolean> body) {
		return new BoolNotTerm(body);
	}

	public static BoolLogicBinaryTerm and(Term<Boolean> left, Term<Boolean> right) {
		return new BoolLogicBinaryTerm(LogicBinaryOperator.AND, left, right);
	}

	public static BoolLogicBinaryTerm or(Term<Boolean> left, Term<Boolean> right) {
		return new BoolLogicBinaryTerm(LogicBinaryOperator.OR, left, right);
	}

	public static BoolLogicBinaryTerm xor(Term<Boolean> left, Term<Boolean> right) {
		return new BoolLogicBinaryTerm(LogicBinaryOperator.XOR, left, right);
	}
}
