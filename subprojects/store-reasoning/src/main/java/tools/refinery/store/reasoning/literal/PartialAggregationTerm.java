/*
 * SPDX-FileCopyrightText: 2025 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning.literal;

import tools.refinery.logic.AbstractValue;
import tools.refinery.logic.Constraint;
import tools.refinery.logic.InvalidQueryException;
import tools.refinery.logic.term.PartialAggregator;
import tools.refinery.logic.equality.LiteralEqualityHelper;
import tools.refinery.logic.equality.LiteralHashCodeHelper;
import tools.refinery.logic.rewriter.TermRewriter;
import tools.refinery.logic.substitution.Substitution;
import tools.refinery.logic.term.*;

import java.util.List;
import java.util.Objects;

public class PartialAggregationTerm<
		A extends AbstractValue<A, C>, C, A2 extends AbstractValue<A2, C2>, C2> extends PartialCallTerm<A> {
	private final PartialAggregator<A, C, A2, C2> aggregator;
	private final Term<A2> body;

	public PartialAggregationTerm(PartialAggregator<A, C, A2, C2> aggregator, Constraint target,
								  List<Variable> arguments, Term<A2> body) {
		super(aggregator.getResultDomain().abstractType(), target, arguments);
		var bodyType = aggregator.getBodyDomain().abstractType();
		if (!body.getType().equals(bodyType)) {
			throw new InvalidQueryException("Expected body %s to be of type %s, got %s instead".formatted(body,
					bodyType.getName(), body.getType().getName()));
		}
		this.aggregator = aggregator;
		this.body = body;
	}

	public PartialAggregator<A, C, A2, C2> getAggregator() {
		return aggregator;
	}

	public Term<A2> getBody() {
		return body;
	}

	@Override
	protected Term<A> doSubstitute(Substitution substitution, List<Variable> substitutedArguments) {
		var substitutedBody = body.substitute(substitution);
		return new PartialAggregationTerm<>(aggregator, getTarget(), substitutedArguments, substitutedBody);
	}

	@Override
	public Term<A> withArguments(Constraint newTarget, List<Variable> newArguments) {
		return new PartialAggregationTerm<>(aggregator, newTarget, newArguments, body);
	}

	@Override
	public boolean equalsWithSubstitution(LiteralEqualityHelper helper, AnyTerm other) {
		if (!super.equalsWithSubstitution(helper, other)) {
			return false;
		}
		var otherAggregationTerm = (PartialAggregationTerm<?, ?, ?, ?>) other;
		return aggregator.equals(otherAggregationTerm.aggregator) &&
				body.equalsWithSubstitution(helper, otherAggregationTerm.body);
	}

	@Override
	public int hashCodeWithSubstitution(LiteralHashCodeHelper helper) {
		return Objects.hash(super.hashCodeWithSubstitution(helper), aggregator, body.hashCodeWithSubstitution(helper));
	}

	@Override
	public Term<A> rewriteSubTerms(TermRewriter termRewriter) {
		return withBody(termRewriter.rewriteTerm(body));
	}

	@Override
	public Term<A> substitute(Substitution substitution) {
		return withBody(body.substitute(substitution));
	}

	public Term<A> withBody(Term<A2> newBody) {
		if (body == newBody) {
			return this;
		}
		return new PartialAggregationTerm<>(aggregator, getTarget(), getArguments(), newBody);
	}

	@Override
	public String toString() {
		var builder = new StringBuilder();
		builder.append("@Partial ");
		builder.append(aggregator);
		builder.append(" { ");
		builder.append(getTarget().toReferenceString());
		builder.append('(');
		var argumentIterator = getArguments().iterator();
		if (argumentIterator.hasNext()) {
			builder.append(argumentIterator.next());
			while (argumentIterator.hasNext()) {
				builder.append(", ").append(argumentIterator.next());
			}
		}
		builder.append(") -> ");
		builder.append(body);
		builder.append(" }");
		return builder.toString();
	}
}
