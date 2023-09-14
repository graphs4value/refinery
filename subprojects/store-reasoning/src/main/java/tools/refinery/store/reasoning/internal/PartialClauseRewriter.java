/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning.internal;

import tools.refinery.store.query.Constraint;
import tools.refinery.store.query.dnf.Dnf;
import tools.refinery.store.query.dnf.DnfBuilder;
import tools.refinery.store.query.dnf.DnfClause;
import tools.refinery.store.query.literal.AbstractCallLiteral;
import tools.refinery.store.query.literal.AbstractCountLiteral;
import tools.refinery.store.query.literal.CallPolarity;
import tools.refinery.store.query.literal.Literal;
import tools.refinery.store.query.term.Aggregator;
import tools.refinery.store.query.term.ConstantTerm;
import tools.refinery.store.query.term.Term;
import tools.refinery.store.query.term.Variable;
import tools.refinery.store.query.term.int_.IntTerms;
import tools.refinery.store.query.term.uppercardinality.UpperCardinalityTerms;
import tools.refinery.store.reasoning.ReasoningAdapter;
import tools.refinery.store.reasoning.literal.*;
import tools.refinery.store.reasoning.representation.PartialRelation;
import tools.refinery.store.reasoning.translator.multiobject.MultiObjectTranslator;
import tools.refinery.store.representation.cardinality.UpperCardinalities;

import java.util.*;
import java.util.function.BinaryOperator;

class PartialClauseRewriter {
	private final PartialQueryRewriter rewriter;
	private final List<Literal> completedLiterals = new ArrayList<>();
	private final Deque<Literal> workList = new ArrayDeque<>();
	private final Set<Variable> positiveVariables = new LinkedHashSet<>();
	private final Set<Variable> unmodifiablePositiveVariables = Collections.unmodifiableSet(positiveVariables);

	public PartialClauseRewriter(PartialQueryRewriter rewriter) {
		this.rewriter = rewriter;
	}

	public List<Literal> rewriteClause(DnfClause clause) {
		workList.addAll(clause.literals());
		while (!workList.isEmpty()) {
			var literal = workList.removeFirst();
			rewrite(literal);
		}
		return completedLiterals;
	}

	private void rewrite(Literal literal) {
		if (!(literal instanceof AbstractCallLiteral callLiteral)) {
			markAsDone(literal);
			return;
		}
		if (callLiteral instanceof CountLowerBoundLiteral countLowerBoundLiteral) {
			rewriteCountLowerBound(countLowerBoundLiteral);
			return;
		}
		if (callLiteral instanceof CountUpperBoundLiteral countUpperBoundLiteral) {
			rewriteCountUpperBound(countUpperBoundLiteral);
			return;
		}
		if (callLiteral instanceof CountCandidateLowerBoundLiteral countCandidateLowerBoundLiteral) {
			rewriteCountCandidateLowerBound(countCandidateLowerBoundLiteral);
			return;
		}
		if (callLiteral instanceof CountCandidateUpperBoundLiteral countCandidateUpperBoundLiteral) {
			rewriteCountCandidateUpperBound(countCandidateUpperBoundLiteral);
			return;
		}
		var target = callLiteral.getTarget();
		if (target instanceof Dnf dnf) {
			rewriteRecursively(callLiteral, dnf);
		} else if (target instanceof ModalConstraint modalConstraint) {
			var modality = modalConstraint.modality();
			var concreteness = modalConstraint.concreteness();
			var constraint = modalConstraint.constraint();
			if (constraint instanceof Dnf dnf) {
				rewriteRecursively(callLiteral, modality, concreteness, dnf);
			} else if (constraint instanceof PartialRelation partialRelation) {
				rewrite(callLiteral, modality, concreteness, partialRelation);
			} else {
				throw new IllegalArgumentException("Cannot interpret modal constraint: " + modalConstraint);
			}
		} else {
			markAsDone(literal);
		}
	}

	private void rewriteCountLowerBound(CountLowerBoundLiteral literal) {
		rewritePartialCount(literal, "lower", Modality.MUST, MultiObjectTranslator.LOWER_CARDINALITY_VIEW, 1,
				IntTerms::mul, IntTerms.INT_SUM);
	}

	private void rewriteCountUpperBound(CountUpperBoundLiteral literal) {
		rewritePartialCount(literal, "upper", Modality.MAY, MultiObjectTranslator.UPPER_CARDINALITY_VIEW,
				UpperCardinalities.ONE, UpperCardinalityTerms::mul, UpperCardinalityTerms.UPPER_CARDINALITY_SUM);
	}

	private <T> void rewritePartialCount(AbstractCountLiteral<T> literal, String name, Modality modality,
										 Constraint view, T one, BinaryOperator<Term<T>> mul, Aggregator<T, T> sum) {
		var type = literal.getResultType();
		var countResult = computeCountVariables(literal, Concreteness.PARTIAL, name);
		var builder = countResult.builder();
		var outputVariable = builder.parameter(type);
		var variablesToCount = countResult.variablesToCount();

		var literals = new ArrayList<Literal>();
		literals.add(new ModalConstraint(modality, Concreteness.PARTIAL, literal.getTarget())
				.call(CallPolarity.POSITIVE, countResult.rewrittenArguments()));
		switch (variablesToCount.size()) {
		case 0 -> literals.add(outputVariable.assign(new ConstantTerm<>(type, one)));
		case 1 -> literals.add(view.call(variablesToCount.get(0),
				outputVariable));
		default -> {
			var firstCount = Variable.of(type);
			literals.add(view.call(variablesToCount.get(0), firstCount));
			int length = variablesToCount.size();
			Term<T> accumulator = firstCount;
			for (int i = 1; i < length; i++) {
				var countVariable = Variable.of(type);
				literals.add(view.call(variablesToCount.get(i), countVariable));
				accumulator = mul.apply(accumulator, countVariable);
			}
			literals.add(outputVariable.assign(accumulator));
		}
		}
		builder.clause(literals);

		var helperQuery = builder.build();
		var aggregationVariable = Variable.of(type);
		var helperArguments = countResult.helperArguments();
		helperArguments.add(aggregationVariable);
		workList.addFirst(literal.getResultVariable().assign(helperQuery.aggregateBy(aggregationVariable, sum,
				helperArguments)));
	}

	private void rewriteCountCandidateLowerBound(CountCandidateLowerBoundLiteral literal) {
		rewriteCandidateCount(literal, "lower", Modality.MAY);
	}

	private void rewriteCountCandidateUpperBound(CountCandidateUpperBoundLiteral literal) {
		rewriteCandidateCount(literal, "upper", Modality.MUST);
	}

	private void rewriteCandidateCount(AbstractCountLiteral<Integer> literal, String name, Modality modality) {
		var countResult = computeCountVariables(literal, Concreteness.CANDIDATE, name);
		var builder = countResult.builder();

		var literals = new ArrayList<Literal>();
		literals.add(new ModalConstraint(modality, Concreteness.CANDIDATE, literal.getTarget())
				.call(CallPolarity.POSITIVE, countResult.rewrittenArguments()));
		for (var variable : countResult.variablesToCount()) {
			literals.add(new ModalConstraint(modality, Concreteness.CANDIDATE, ReasoningAdapter.EXISTS_SYMBOL)
					.call(variable));
		}
		builder.clause(literals);

		var helperQuery = builder.build();
		workList.addFirst(literal.getResultVariable().assign(helperQuery.count(countResult.helperArguments())));
	}

	private CountResult computeCountVariables(AbstractCountLiteral<?> literal, Concreteness concreteness,
											  String name) {
		var target = literal.getTarget();
		int arity = target.arity();
		var parameters = target.getParameters();
		var literalArguments = literal.getArguments();
		var privateVariables = literal.getPrivateVariables(positiveVariables);
		var builder = Dnf.builder("%s#%s#%s".formatted(target.name(), concreteness, name));
		var rewrittenArguments = new ArrayList<Variable>(parameters.size());
		var variablesToCount = new ArrayList<Variable>();
		var helperArguments = new ArrayList<Variable>();
		var literalToRewrittenArgumentMap = new HashMap<Variable, Variable>();
		for (int i = 0; i < arity; i++) {
			var literalArgument = literalArguments.get(i);
			var parameter = parameters.get(i);
			var rewrittenArgument = literalToRewrittenArgumentMap.computeIfAbsent(literalArgument, key -> {
				helperArguments.add(key);
				var newArgument = builder.parameter(parameter);
				if (privateVariables.contains(key)) {
					variablesToCount.add(newArgument);
				}
				return newArgument;
			});
			rewrittenArguments.add(rewrittenArgument);
		}
		return new CountResult(builder, rewrittenArguments, helperArguments, variablesToCount);
	}

	private void markAsDone(Literal literal) {
		completedLiterals.add(literal);
		positiveVariables.addAll(literal.getOutputVariables());
	}

	private void rewriteRecursively(AbstractCallLiteral callLiteral, Modality modality, Concreteness concreteness,
									Dnf dnf) {
		var liftedDnf = rewriter.getLifter().lift(modality, concreteness, dnf);
		rewriteRecursively(callLiteral, liftedDnf);
	}

	private void rewriteRecursively(AbstractCallLiteral callLiteral, Dnf dnf) {
		var rewrittenDnf = rewriter.rewrite(dnf);
		var rewrittenLiteral = callLiteral.withTarget(rewrittenDnf);
		markAsDone(rewrittenLiteral);
	}

	private void rewrite(AbstractCallLiteral callLiteral, Modality modality, Concreteness concreteness,
						 PartialRelation partialRelation) {
		var relationRewriter = rewriter.getRelationRewriter(partialRelation);
		var literals = relationRewriter.rewriteLiteral(
				unmodifiablePositiveVariables, callLiteral, modality, concreteness);
		int length = literals.size();
		for (int i = length - 1; i >= 0; i--) {
			workList.addFirst(literals.get(i));
		}
	}

	private record CountResult(DnfBuilder builder, List<Variable> rewrittenArguments, List<Variable> helperArguments,
							   List<Variable> variablesToCount) {
	}
}
