/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.logic.term;

import tools.refinery.logic.InvalidQueryException;
import tools.refinery.logic.equality.LiteralEqualityHelper;
import tools.refinery.logic.equality.LiteralHashCodeHelper;
import tools.refinery.logic.rewriter.TermRewriter;
import tools.refinery.logic.substitution.Substitution;
import tools.refinery.logic.valuation.Valuation;

import java.util.*;

// {@link Object#equals(Object)} is implemented by {@link AbstractTerm}.
@SuppressWarnings("squid:S2160")
public abstract class BinaryTerm<R, T1, T2> extends AbstractTerm<R> {
	private final Class<T1> leftType;
	private final Class<T2> rightType;
	private final Term<T1> left;
	private final Term<T2> right;

	protected BinaryTerm(Class<R> type, Class<T1> leftType, Class<T2> rightType, Term<T1> left, Term<T2> right) {
		super(type);
		if (!left.getType().equals(leftType)) {
			throw new InvalidQueryException("Expected left %s to be of type %s, got %s instead".formatted(
					left, leftType.getName(), left.getType().getName()));
		}
		if (!right.getType().equals(rightType)) {
			throw new InvalidQueryException("Expected right %s to be of type %s, got %s instead".formatted(
					right, rightType.getName(), right.getType().getName()));
		}
		this.leftType = leftType;
		this.rightType = rightType;
		this.left = left;
		this.right = right;
	}

	public Class<T1> getLeftType() {
		return leftType;
	}

	public Class<T2> getRightType() {
		return rightType;
	}

	public Term<T1> getLeft() {
		return left;
	}

	public Term<T2> getRight() {
		return right;
	}

	@Override
	public R evaluate(Valuation valuation) {
		var leftValue = left.evaluate(valuation);
		if (leftValue == null) {
			return null;
		}
		var rightValue = right.evaluate(valuation);
		if (rightValue == null) {
			return null;
		}
		return doEvaluate(leftValue, rightValue);
	}

	protected abstract R doEvaluate(T1 leftValue, T2 rightValue);

	@Override
	public boolean equalsWithSubstitution(LiteralEqualityHelper helper, AnyTerm other) {
		if (!super.equalsWithSubstitution(helper, other)) {
			return false;
		}
		var otherBinaryTerm = (BinaryTerm<?, ?, ?>) other;
		return leftType.equals(otherBinaryTerm.leftType) &&
				rightType.equals(otherBinaryTerm.rightType) &&
				left.equalsWithSubstitution(helper, otherBinaryTerm.left) &&
				right.equalsWithSubstitution(helper, otherBinaryTerm.right);
	}

	@Override
	public int hashCodeWithSubstitution(LiteralHashCodeHelper helper) {
		return Objects.hash(super.hashCodeWithSubstitution(helper), leftType.hashCode(), rightType.hashCode(),
				left.hashCodeWithSubstitution(helper), right.hashCodeWithSubstitution(helper));
	}

	@Override
	public Term<R> rewriteSubTerms(TermRewriter termRewriter) {
		return withSubTerms(left.rewriteSubTerms(termRewriter), right.rewriteSubTerms(termRewriter));
	}

	@Override
	public Term<R> substitute(Substitution substitution) {
		return withSubTerms(left.substitute(substitution), right.substitute(substitution));
	}

	public abstract Term<R> withSubTerms(Term<T1> newLeft, Term<T2> newRight);

	@Override
	public Set<Variable> getVariables() {
		var variables = new LinkedHashSet<>(left.getVariables());
		variables.addAll(right.getVariables());
		return Collections.unmodifiableSet(variables);
	}

	@Override
	public Set<Variable> getInputVariables(Set<? extends Variable> positiveVariablesInClause) {
		var inputVariables = new LinkedHashSet<>(left.getInputVariables(positiveVariablesInClause));
		inputVariables.addAll(right.getInputVariables(positiveVariablesInClause));
		return Collections.unmodifiableSet(inputVariables);
	}

	@Override
	public Set<Variable> getPrivateVariables(Set<? extends Variable> positiveVariablesInClause) {
		var privateVariables = new LinkedHashSet<>(left.getPrivateVariables(positiveVariablesInClause));
		var rightPrivateVariables = right.getPrivateVariables(positiveVariablesInClause);
		for (var rightPrivateVariable : rightPrivateVariables) {
			if (privateVariables.contains(rightPrivateVariable)) {
				throw new InvalidQueryException("Private variables %s occurs of both sides of %s."
						.formatted(rightPrivateVariable, this));
			}
		}
		return Collections.unmodifiableSet(privateVariables);
	}
}
