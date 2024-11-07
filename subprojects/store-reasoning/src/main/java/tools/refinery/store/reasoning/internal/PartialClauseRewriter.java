/*
 * SPDX-FileCopyrightText: 2023-2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning.internal;

import org.jetbrains.annotations.Nullable;
import tools.refinery.logic.Constraint;
import tools.refinery.logic.dnf.Dnf;
import tools.refinery.logic.dnf.DnfBuilder;
import tools.refinery.logic.dnf.DnfClause;
import tools.refinery.logic.literal.*;
import tools.refinery.logic.term.*;
import tools.refinery.logic.term.int_.IntTerms;
import tools.refinery.logic.term.uppercardinality.UpperCardinalities;
import tools.refinery.logic.term.uppercardinality.UpperCardinalityTerms;
import tools.refinery.logic.util.CircularReferenceException;
import tools.refinery.store.reasoning.ReasoningAdapter;
import tools.refinery.store.reasoning.literal.*;
import tools.refinery.store.reasoning.representation.PartialRelation;
import tools.refinery.store.reasoning.translator.TranslationException;
import tools.refinery.store.reasoning.translator.multiobject.MultiObjectTranslator;

import java.util.*;
import java.util.function.BinaryOperator;

class PartialClauseRewriter {
	private final PartialQueryRewriter rewriter;
	private final List<Literal> completedLiterals = new ArrayList<>();
	private final Deque<Literal> workList = new ArrayDeque<>();
	private final Map<Literal, PartialRelation> trace = new LinkedHashMap<>();
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
		switch (literal) {
		case AbstractCallLiteral callLiteral -> rewriteCallLiteral(callLiteral);
		case TermLiteral<?> termLiteral -> rewriteTermLiteral(termLiteral);
		default -> markAsDone(literal);
		}
	}

	private void rewriteCallLiteral(AbstractCallLiteral callLiteral) {
		switch (callLiteral) {
		case CountLowerBoundLiteral countLowerBoundLiteral -> rewriteCountLowerBound(countLowerBoundLiteral);
		case CountUpperBoundLiteral countUpperBoundLiteral -> rewriteCountUpperBound(countUpperBoundLiteral);
		case CountCandidateLowerBoundLiteral countCandidateLowerBoundLiteral ->
				rewriteCountCandidateLowerBound(countCandidateLowerBoundLiteral);
		case CountCandidateUpperBoundLiteral countCandidateUpperBoundLiteral ->
				rewriteCountCandidateUpperBound(countCandidateUpperBoundLiteral);
		default -> {
			var target = callLiteral.getTarget();
			switch (target) {
			case Dnf dnf -> rewriteRecursively(callLiteral, dnf);
			case ModalConstraint modalConstraint -> {
				var modality = modalConstraint.modality().toModality();
				var concreteness = modalConstraint.concreteness().toConcreteness();
				var constraint = modalConstraint.constraint();
				switch (constraint) {
				case Dnf dnf -> rewriteRecursively(callLiteral, modality, concreteness, dnf);
				case PartialRelation partialRelation -> rewrite(callLiteral, modality, concreteness, partialRelation);
				default -> throw new IllegalArgumentException("Cannot interpret modal constraint: " + modalConstraint);
				}
			}
			default -> markAsDone(callLiteral);
			}
		}
		}
	}

	private <T> void rewriteTermLiteral(TermLiteral<T> termLiteral) {
		var term = termLiteral.getTerm();
		var rewrittenTerm = rewriteTerm(term);
		if (term == rewrittenTerm) {
			markAsDone(termLiteral);
			return;
		}
		var rewrittenLiteral = termLiteral.withTerm(rewrittenTerm);
		workList.addFirst(rewrittenLiteral);
	}

	private <T> Term<T> rewriteTerm(Term<T> term) {
		var termWithProcessedSubTerms = term.rewriteSubTerms(this::rewriteTerm);
		if (!(termWithProcessedSubTerms instanceof AbstractCallTerm<T> callTerm)) {
			return termWithProcessedSubTerms;
		}
		var target = callTerm.getTarget();
		return switch (target) {
			case Dnf dnf -> callTerm.withTarget(rewriter.rewrite(dnf));
			case ModalConstraint modalConstraint -> {
				var modality = modalConstraint.modality().toModality();
				var concreteness = modalConstraint.concreteness().toConcreteness();
				var constraint = modalConstraint.constraint();
				yield switch (constraint) {
					case Dnf dnf -> {
						var newTarget = rewriter.getLifter().lift(modality, concreteness, dnf);
						yield callTerm.withTarget(rewriter.rewrite(newTarget));
					}
					case PartialRelation partialRelation -> {
						var relationRewriter = rewriter.getRelationRewriter(partialRelation);
						yield relationRewriter.rewriteTerm(callTerm, modality, concreteness);
					}
					default -> throw new IllegalArgumentException("Cannot interpret modal constraint: " +
							modalConstraint);
				};
			}
			default -> term;
		};
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
		literals.add(ModalConstraint.of(modality, Concreteness.PARTIAL, literal.getTarget())
				.call(CallPolarity.POSITIVE, countResult.rewrittenArguments()));
		switch (variablesToCount.size()) {
		case 0 -> literals.add(outputVariable.assign(new ConstantTerm<>(type, one)));
		case 1 -> literals.add(view.call(variablesToCount.getFirst(),
				outputVariable));
		default -> {
			var firstCount = Variable.of(type);
			literals.add(view.call(variablesToCount.getFirst(), firstCount));
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
		addWithTrace(literal.getResultVariable().assign(helperQuery.aggregateBy(aggregationVariable, sum,
				helperArguments)), literal);
	}

	private void addWithTrace(Literal newLiteral, @Nullable PartialRelation traceRelation) {
		if (traceRelation != null) {
			trace.put(newLiteral, traceRelation);
		}
		workList.addFirst(newLiteral);
	}

	private void addWithTrace(Literal newLiteral, Literal previousLiteral) {
		var previousTrace = trace.get(previousLiteral);
		addWithTrace(newLiteral, previousTrace);
	}

	private void rewriteCountCandidateLowerBound(CountCandidateLowerBoundLiteral literal) {
		rewriteCandidateCount(literal, "lower", Modality.MUST);
	}

	private void rewriteCountCandidateUpperBound(CountCandidateUpperBoundLiteral literal) {
		rewriteCandidateCount(literal, "upper", Modality.MAY);
	}

	private void rewriteCandidateCount(AbstractCountLiteral<Integer> literal, String name, Modality modality) {
		var countResult = computeCountVariables(literal, Concreteness.CANDIDATE, name);
		var builder = countResult.builder();

		var literals = new ArrayList<Literal>();
		literals.add(ModalConstraint.of(modality, Concreteness.CANDIDATE, literal.getTarget())
				.call(CallPolarity.POSITIVE, countResult.rewrittenArguments()));
		for (var variable : countResult.variablesToCount()) {
			literals.add(ModalConstraint.of(modality, Concreteness.CANDIDATE, ReasoningAdapter.EXISTS_SYMBOL)
					.call(variable));
		}
		builder.clause(literals);

		var helperQuery = builder.build();
		addWithTrace(literal.getResultVariable().assign(helperQuery.count(countResult.helperArguments())), literal);
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
		Dnf rewrittenDnf;
		try {
			rewrittenDnf = rewriter.rewrite(dnf);
		} catch (CircularReferenceException e) {
			var traceRelation = trace.get(callLiteral);
			throw new TranslationException(traceRelation, e);
		}
		var rewrittenLiteral = callLiteral.withTarget(rewrittenDnf);
		markAsDone(rewrittenLiteral);
	}

	private void rewrite(AbstractCallLiteral callLiteral, Modality modality, Concreteness concreteness,
						 PartialRelation partialRelation) {
		var relationRewriter = rewriter.getRelationRewriter(partialRelation);
		var literals = relationRewriter.rewriteLiteral(unmodifiablePositiveVariables, callLiteral, modality,
				concreteness);
		int length = literals.size();
		for (int i = length - 1; i >= 0; i--) {
			addWithTrace(literals.get(i), partialRelation);
		}
	}

	private record CountResult(DnfBuilder builder, List<Variable> rewrittenArguments, List<Variable> helperArguments,
							   List<Variable> variablesToCount) {
	}
}
