/*
 * SPDX-FileCopyrightText: 2023-2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning.internal;

import org.jetbrains.annotations.Nullable;
import tools.refinery.logic.AbstractValue;
import tools.refinery.logic.Constraint;
import tools.refinery.logic.dnf.Dnf;
import tools.refinery.logic.dnf.DnfBuilder;
import tools.refinery.logic.dnf.DnfClause;
import tools.refinery.logic.dnf.SymbolicParameter;
import tools.refinery.logic.literal.*;
import tools.refinery.logic.term.*;
import tools.refinery.logic.term.abstractdomain.AbstractDomainTerms;
import tools.refinery.logic.term.int_.IntTerms;
import tools.refinery.logic.term.intinterval.IntInterval;
import tools.refinery.logic.term.intinterval.IntIntervalDomain;
import tools.refinery.logic.term.intinterval.IntIntervalTerms;
import tools.refinery.logic.term.truthvalue.TruthValue;
import tools.refinery.logic.term.truthvalue.TruthValueTerms;
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
				var modality = modalConstraint.modality();
				var concreteness = modalConstraint.concreteness();
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
		return switch (termWithProcessedSubTerms) {
			case ReifyTerm reifyTerm -> {
				// {@link ReifyTerm} implements {@code Term<TruthValue>}, so {@code T} can only be {@link TruthValue}.
				@SuppressWarnings("unchecked")
				var result = (Term<T>) rewriteReifyTerm(reifyTerm);
				yield result;
			}
			case PartialCountTerm partialCountTerm -> {
				// {@link PartialCountTerm} implements {@code Term<IntInterval>}, so {@code T} can only be
				// {@link IntInterval}.
				@SuppressWarnings("unchecked")
				var result = (Term<T>) rewritePartialCountTerm(partialCountTerm);
				yield result;
			}
			case PartialAggregationTerm<?, ?, ?, ?> partialAggregationTerm -> {
				// Rewriting doesn't change a term's type, but we can't express the generic bound
				// {@code PartialAggregationTerm<T extends AbstractValue<T, C>, C, ?, ?>} in Java, so we have to cast
				// explicitly.
				@SuppressWarnings("unchecked")
				var result = (Term<T>) rewritePartialAggregationTerm(partialAggregationTerm);
				yield result;
			}
			case AbstractCallTerm<T> abstractCallTerm -> rewriteCallTerm(abstractCallTerm);
			case PartialFunctionCallTerm<?, ?> partialFunctionCallTerm -> {
				// Rewriting doesn't change a term's type, but we can't express the generic bound
				// {@code PartialFunctionCallTerm<T extends AbstractValue<T, C>, C>} in Java, so we have to cast
				// explicitly.
				@SuppressWarnings("unchecked")
				var result = (Term<T>) rewritePartialFunctionCallTerm(partialFunctionCallTerm);
				yield result;
			}
			default -> termWithProcessedSubTerms;
		};
	}

	private Term<TruthValue> rewriteReifyTerm(ReifyTerm reifyTerm) {
		var target = reifyTerm.getTarget();
		var concreteness = reifyTerm.getConcreteness();
		var countResult = computeCountVariables(reifyTerm, "reificationHelper", false);
		var helper = countResult.builder()
				.clause(target.call(CallPolarity.POSITIVE, countResult.rewrittenArguments()))
				.build();
		var output = Variable.of("output", TruthValue.class);
		var mayTarget = ModalConstraint.of(ModalitySpecification.MAY, concreteness, helper);
		var mustTarget = ModalConstraint.of(ModalitySpecification.MUST, concreteness, helper);
		var arguments = countResult.helperArguments();
		var reification = Dnf.builder("%s#reified#%s".formatted(target.name(), concreteness))
				.symbolicParameters(helper.getSymbolicParameters())
				.parameter(output)
				.clause(
						mayTarget.call(CallPolarity.POSITIVE, arguments),
						mustTarget.call(CallPolarity.POSITIVE, arguments),
						output.assign(TruthValueTerms.constant(TruthValue.TRUE))
				)
				.clause(
						mayTarget.call(CallPolarity.POSITIVE, arguments),
						mustTarget.call(CallPolarity.NEGATIVE, arguments),
						output.assign(TruthValueTerms.constant(TruthValue.UNKNOWN))
				)
				.clause(
						mayTarget.call(CallPolarity.NEGATIVE, arguments),
						mustTarget.call(CallPolarity.POSITIVE, arguments),
						output.assign(TruthValueTerms.constant(TruthValue.ERROR))
				)
				.build();
		var reifiedArguments = new ArrayList<Variable>(arguments.size() + 1);
		reifiedArguments.addAll(arguments);
		reifiedArguments.add(output);
		return reification.leftJoinBy(output, TruthValue.FALSE, List.copyOf(reifiedArguments));
	}

	private Term<IntInterval> rewritePartialCountTerm(PartialCountTerm partialCountTerm) {
		var constraint = createPartialCountConstraint(partialCountTerm);
		var symbolicParameters = constraint.getSymbolicParameters();
		var outputVariable = symbolicParameters.getLast().getVariable().asDataVariable(IntInterval.class);
		var arguments = symbolicParameters.stream().map(SymbolicParameter::getVariable).toList();
		return constraint.aggregateBy(outputVariable, IntIntervalTerms.INT_INTERVAL_SUM, arguments);
	}

	private Dnf createPartialCountConstraint(PartialCallTerm<?> partialCallTerm) {
		var target = partialCallTerm.getTarget();
		var concreteness = partialCallTerm.getConcreteness();
		var countResult = computeCountVariables(partialCallTerm, "partialCount", true);
		var variablesToCount = countResult.variablesToCount();
		Term<IntInterval> productTerm;
		if (variablesToCount.isEmpty()) {
			productTerm = IntIntervalTerms.constant(IntInterval.ONE);
		} else {
			int length = variablesToCount.size();
			productTerm = ReasoningAdapter.COUNT_SYMBOL.call(concreteness, List.of(variablesToCount.getFirst()));
			for (int i = 1; i < length; i++) {
				productTerm = IntIntervalTerms.mul(productTerm,
						ReasoningAdapter.COUNT_SYMBOL.call(concreteness, List.of(variablesToCount.get(i))));
			}
		}
		var mayTarget = ModalConstraint.of(ModalitySpecification.MAY, concreteness, target);
		var mustTarget = ModalConstraint.of(ModalitySpecification.MUST, concreteness, target);
		var arguments = countResult.rewrittenArguments();
		var output = Variable.of("output", IntInterval.class);
		return countResult.builder()
				.parameter(output)
				.clause(
						mayTarget.call(CallPolarity.POSITIVE, arguments),
						mustTarget.call(CallPolarity.POSITIVE, arguments),
						output.assign(productTerm)
				)
				.clause(
						mayTarget.call(CallPolarity.POSITIVE, arguments),
						mustTarget.call(CallPolarity.NEGATIVE, arguments),
						output.assign(AbstractDomainTerms.join(IntIntervalDomain.INSTANCE,
								IntIntervalTerms.constant(IntInterval.ZERO), productTerm))
				)
				.clause(
						mayTarget.call(CallPolarity.NEGATIVE, arguments),
						mustTarget.call(CallPolarity.POSITIVE, arguments),
						output.assign(AbstractDomainTerms.meet(IntIntervalDomain.INSTANCE,
								IntIntervalTerms.constant(IntInterval.ZERO), productTerm))
				)
				.build();
	}

	private <A extends AbstractValue<A, C>, C, A2 extends AbstractValue<A2, C2>, C2> Term<A>
	rewritePartialAggregationTerm(PartialAggregationTerm<A, C, A2, C2> partialAggregationTerm) {
		return switch (partialAggregationTerm.getAggregator()) {
			case PartialAggregator.MultiplicitySensitive<A, C, A2, C2, ?> multiplicitySensitiveAggregator ->
					rewritePartialAggregationTerm(partialAggregationTerm, multiplicitySensitiveAggregator);
			case PartialAggregator.MultiplicityInsensitive<A, C, A2, C2> multiplicityInsensitiveAggregator ->
					rewritePartialAggregationTerm(partialAggregationTerm, multiplicityInsensitiveAggregator);
			case PartialAggregator.MeetAggregator<?, ?> meetAggregator -> {
				// These types are forced by {@code MeetAggregator}.
				@SuppressWarnings("unchecked")
				var uncheckedTerm = (PartialAggregationTerm<A, C, A, C>) partialAggregationTerm;
				@SuppressWarnings("unchecked")
				var uncheckedAggregator = (Aggregator<A, A>) meetAggregator.getInnerAggregator();
				yield rewriteLatticeAggregationTerm(uncheckedTerm, Modality.MUST, uncheckedAggregator);
			}
			case PartialAggregator.JoinAggregator<?, ?> joinAggregator -> {
				// These types are forced by {@code JoinAggregator}.
				@SuppressWarnings("unchecked")
				var uncheckedTerm = (PartialAggregationTerm<A, C, A, C>) partialAggregationTerm;
				@SuppressWarnings("unchecked")
				var uncheckedAggregator = (Aggregator<A, A>) joinAggregator.getInnerAggregator();
				yield rewriteLatticeAggregationTerm(uncheckedTerm, Modality.MAY, uncheckedAggregator);
			}
		};
	}

	private <A extends AbstractValue<A, C>, C, A2 extends AbstractValue<A2, C2>, C2, T> Term<A>
	rewritePartialAggregationTerm(PartialAggregationTerm<A, C, A2, C2> partialAggregationTerm,
								  PartialAggregator.MultiplicitySensitive<A, C, A2, C2, T> partialAggregator) {
		var constraint = createPartialCountConstraint(partialAggregationTerm);
		var symbolicParameters = new ArrayList<>(constraint.getSymbolicParameters());
		var output = Variable.of("output", partialAggregator.getIntermediateType());
		symbolicParameters.set(symbolicParameters.size() - 1, new SymbolicParameter(output, ParameterDirection.OUT));
		var rawCount = Variable.of("rawCount", IntInterval.class);
		var arguments = symbolicParameters.stream().map(SymbolicParameter::getVariable).toList();
		var rawArguments = new ArrayList<>(arguments);
		rawArguments.set(rawArguments.size() - 1, rawCount);
		var target = partialAggregationTerm.getTarget();
		var concreteness = partialAggregationTerm.getConcreteness();
		var body = partialAggregationTerm.getBody();
		var helper = Dnf.builder("%s#aggregationHelper#%s".formatted(target.name(), concreteness))
				.symbolicParameters(symbolicParameters)
				.clause(
						constraint.call(CallPolarity.POSITIVE, rawArguments),
						output.assign(partialAggregator.withWeight(rawCount, body))
				)
				.build();
		return helper.aggregateBy(output, partialAggregator.getInnerAggregator(), arguments);
	}

	private <A extends AbstractValue<A, C>, C, A2 extends AbstractValue<A2, C2>, C2> Term<A>
	rewritePartialAggregationTerm(PartialAggregationTerm<A, C, A2, C2> partialAggregationTerm,
								  PartialAggregator.MultiplicityInsensitive<A, C, A2, C2> partialAggregator) {
		var target = partialAggregationTerm.getTarget();
		var concreteness = partialAggregationTerm.getConcreteness();
		var reificationHelper = createCardinalityInsensitiveReificationHelper(partialAggregationTerm);
		var bodyDomain = partialAggregator.getBodyDomain();
		var bodyType = bodyDomain.abstractType();
		var output = Variable.of("output", bodyType);
		var mayTarget = ModalConstraint.of(ModalitySpecification.MAY, concreteness, reificationHelper);
		var mustTarget = ModalConstraint.of(ModalitySpecification.MUST, concreteness, reificationHelper);
		var arguments = reificationHelper.getSymbolicParameters().stream()
				.map(SymbolicParameter::getVariable)
				.toList();
		var body = partialAggregationTerm.getBody();
		var neutralElement = new ConstantTerm<>(bodyType, partialAggregator.getNeutralElement());
		var helper = Dnf.builder("%s#aggregationHelper#%s".formatted(target.name(), concreteness))
				.symbolicParameters(reificationHelper.getSymbolicParameters())
				.parameter(output)
				.clause(
						mayTarget.call(CallPolarity.POSITIVE, arguments),
						mustTarget.call(CallPolarity.POSITIVE, arguments),
						output.assign(body)
				)
				.clause(
						mayTarget.call(CallPolarity.POSITIVE, arguments),
						mustTarget.call(CallPolarity.NEGATIVE, arguments),
						output.assign(AbstractDomainTerms.join(bodyDomain, neutralElement, body))
				)
				.clause(
						mayTarget.call(CallPolarity.NEGATIVE, arguments),
						mustTarget.call(CallPolarity.POSITIVE, arguments),
						output.assign(AbstractDomainTerms.meet(bodyDomain, neutralElement, body))
				)
				.build();
		var argumentsWithOutput = new ArrayList<Variable>(arguments.size() + 1);
		argumentsWithOutput.addAll(arguments);
		argumentsWithOutput.add(output);
		return helper.aggregateBy(output, partialAggregator.getInnerAggregator(), argumentsWithOutput);
	}

	private Dnf createCardinalityInsensitiveReificationHelper(PartialCallTerm<?> callTerm) {
		var target = callTerm.getTarget();
		var countResult = computeCountVariables(callTerm, "reificationHelper", true);
		var variablesToCount = countResult.variablesToCount();
		var literals = new ArrayList<Literal>(variablesToCount.size() + 1);
		literals.add(target.call(CallPolarity.POSITIVE, countResult.rewrittenArguments()));
		for (var variable : variablesToCount) {
			literals.add(ReasoningAdapter.EXISTS_SYMBOL.call(variable));
		}
		return countResult.builder()
				.clause(literals)
				.build();
	}

	private <A extends AbstractValue<A, C>, C> Term<A> rewriteLatticeAggregationTerm(
			PartialAggregationTerm<A, C, A, C> partialAggregationTerm, Modality modality,
			Aggregator<A, A> innerAggregator) {
		var target = partialAggregationTerm.getTarget();
		var concreteness = partialAggregationTerm.getConcreteness();
		var reificationHelper = createCardinalityInsensitiveReificationHelper(partialAggregationTerm);
		var body = partialAggregationTerm.getBody();
		var output = Variable.of("output", body.getType());
		var modalTarget = ModalConstraint.of(modality.toSpecification(), concreteness, reificationHelper);
		var arguments = reificationHelper.getSymbolicParameters().stream()
				.map(SymbolicParameter::getVariable)
				.toList();
		var helper = Dnf.builder("%s#aggregationHelper#%s".formatted(target.name(), concreteness))
				.symbolicParameters(reificationHelper.getSymbolicParameters())
				.parameter(output)
				.clause(
						modalTarget.call(CallPolarity.POSITIVE, arguments),
						output.assign(body)
				)
				.build();
		var argumentsWithOutput = new ArrayList<Variable>(arguments.size() + 1);
		argumentsWithOutput.addAll(arguments);
		argumentsWithOutput.add(output);
		return helper.aggregateBy(output, innerAggregator, argumentsWithOutput);
	}

	private <T> Term<T> rewriteCallTerm(AbstractCallTerm<T> callTerm) {
		var target = callTerm.getTarget();
		return switch (target) {
			case Dnf dnf -> callTerm.withTarget(rewriter.rewrite(dnf));
			case ModalConstraint modalConstraint -> {
				var modality = modalConstraint.modality();
				var concreteness = modalConstraint.concreteness();
				var constraint = modalConstraint.constraint();
				yield switch (constraint) {
					case Dnf dnf -> {
						var newTarget = rewriter.getLifter().lift(modality, concreteness, dnf);
						yield callTerm.withTarget(rewriter.rewrite(newTarget));
					}
					case PartialRelation partialRelation -> {
						var relationRewriter = rewriter.getRelationRewriter(partialRelation);
						var term = relationRewriter.rewriteTerm(callTerm, modality.toModality(),
                                concreteness.toConcreteness());
						yield rewriteTerm(term);
					}
					default -> throw new IllegalArgumentException("Cannot interpret modal constraint: " +
							modalConstraint);
				};
			}
			default -> callTerm;
		};
	}

	private <A extends AbstractValue<A, C>, C> Term<A> rewritePartialFunctionCallTerm(
			PartialFunctionCallTerm<A, C> partialFunctionCallTerm) {
		var functionRewriter = rewriter.getFunctionRewriter(partialFunctionCallTerm.getPartialFunction());
		var concreteness = partialFunctionCallTerm.getConcreteness().toConcreteness();
		var term = functionRewriter.rewritePartialFunctionCall(concreteness, partialFunctionCallTerm.getArguments());
		return rewriteTerm(term);
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
		return computeCountVariables(literal.getTarget(), literal.getArguments(),
				literal.getPrivateVariables(unmodifiablePositiveVariables), "%s#%s".formatted(name, concreteness),
				true);
	}

	private CountResult computeCountVariables(PartialCallTerm<?> term, String name, boolean exposePrivateVariables) {
		return computeCountVariables(term.getTarget(), term.getArguments(),
				term.getPrivateVariables(unmodifiablePositiveVariables), "%s#%s".formatted(
						name, term.getConcreteness()), exposePrivateVariables);
	}

	private CountResult computeCountVariables(
			Constraint target, List<Variable> literalArguments, Set<Variable> privateVariables, String name,
			boolean exposePrivateVariables) {
		int arity = target.arity();
		var parameters = target.getParameters();
		var builder = Dnf.builder("%s#%s".formatted(target.name(), name));
		var rewrittenArguments = new ArrayList<Variable>(parameters.size());
		var variablesToCount = new ArrayList<NodeVariable>();
		var helperArguments = new ArrayList<Variable>();
		var literalToRewrittenArgumentMap = new HashMap<Variable, Variable>();
		for (int i = 0; i < arity; i++) {
			var literalArgument = literalArguments.get(i);
			var parameter = parameters.get(i);
			var rewrittenArgument = literalToRewrittenArgumentMap.computeIfAbsent(literalArgument, key -> {
				boolean isPrivate = privateVariables.contains(key);
				if (exposePrivateVariables || !isPrivate) {
					helperArguments.add(key);
					builder.parameter(key, parameter.getDirection());
				}
				if (isPrivate && key.isNodeVariable()) {
					variablesToCount.add(key.asNodeVariable());
				}
				return key;
			});
			rewrittenArguments.add(rewrittenArgument);
		}
		return new CountResult(builder, rewrittenArguments, helperArguments, variablesToCount);
	}

	private void markAsDone(Literal literal) {
		completedLiterals.add(literal);
		positiveVariables.addAll(literal.getOutputVariables());
	}

	private void rewriteRecursively(AbstractCallLiteral callLiteral, ModalitySpecification modality,
									ConcretenessSpecification concreteness, Dnf dnf) {
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

	private void rewrite(AbstractCallLiteral callLiteral, ModalitySpecification modality,
						 ConcretenessSpecification concreteness, PartialRelation partialRelation) {
		if (modality == ModalitySpecification.UNSPECIFIED || concreteness == ConcretenessSpecification.UNSPECIFIED) {
			markAsDone(callLiteral);
			return;
		}
		var relationRewriter = rewriter.getRelationRewriter(partialRelation);
		var literals = relationRewriter.rewriteLiteral(unmodifiablePositiveVariables, callLiteral,
				modality.toModality(), concreteness.toConcreteness());
		int length = literals.size();
		for (int i = length - 1; i >= 0; i--) {
			addWithTrace(literals.get(i), partialRelation);
		}
	}

	private record CountResult(DnfBuilder builder, List<Variable> rewrittenArguments, List<Variable> helperArguments,
							   List<NodeVariable> variablesToCount) {
	}
}
