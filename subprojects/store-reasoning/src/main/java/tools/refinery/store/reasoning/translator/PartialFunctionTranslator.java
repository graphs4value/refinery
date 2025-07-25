/*
 * SPDX-FileCopyrightText: 2025 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning.translator;

import tools.refinery.logic.AbstractValue;
import tools.refinery.logic.Constraint;
import tools.refinery.logic.dnf.FunctionalQuery;
import tools.refinery.logic.dnf.Query;
import tools.refinery.logic.dnf.QueryBuilder;
import tools.refinery.logic.dnf.RelationalQuery;
import tools.refinery.logic.literal.Literal;
import tools.refinery.logic.term.DataVariable;
import tools.refinery.logic.term.NodeVariable;
import tools.refinery.logic.term.ParameterDirection;
import tools.refinery.logic.term.Variable;
import tools.refinery.logic.term.abstractdomain.AbstractDomainTerms;
import tools.refinery.store.dse.propagation.PropagationBuilder;
import tools.refinery.store.dse.transition.Rule;
import tools.refinery.store.dse.transition.RuleBuilder;
import tools.refinery.store.dse.transition.objectives.Criteria;
import tools.refinery.store.dse.transition.objectives.Objectives;
import tools.refinery.store.model.ModelStoreBuilder;
import tools.refinery.store.query.view.FunctionView;
import tools.refinery.store.reasoning.ReasoningAdapter;
import tools.refinery.store.reasoning.ReasoningBuilder;
import tools.refinery.store.reasoning.actions.PartialActionLiterals;
import tools.refinery.store.reasoning.interpretation.PartialFunctionRewriter;
import tools.refinery.store.reasoning.interpretation.PartialInterpretation;
import tools.refinery.store.reasoning.interpretation.QueryBasedFunctionInterpretationFactory;
import tools.refinery.store.reasoning.interpretation.QueryBasedFunctionRewriter;
import tools.refinery.store.reasoning.literal.*;
import tools.refinery.store.reasoning.refinement.ConcreteSymbolRefiner;
import tools.refinery.store.reasoning.refinement.PartialInterpretationRefiner;
import tools.refinery.store.reasoning.refinement.StorageRefiner;
import tools.refinery.store.reasoning.representation.PartialFunction;
import tools.refinery.store.reasoning.representation.PartialRelation;
import tools.refinery.store.representation.AnySymbol;
import tools.refinery.store.representation.Symbol;

import java.util.ArrayList;
import java.util.function.BiConsumer;

import static tools.refinery.logic.literal.Literals.check;
import static tools.refinery.logic.literal.Literals.not;
import static tools.refinery.store.reasoning.literal.PartialLiterals.*;

@SuppressWarnings("UnusedReturnValue")
public final class PartialFunctionTranslator<A extends AbstractValue<A, C>, C>
		extends PartialSymbolTranslator<A, C> {

	private final PartialFunction<A, C> partialFunction;
	private Constraint domainConstraint;
	private PartialFunctionRewriter<A, C> rewriter;
	private FunctionalQuery<A> query;
	private FunctionalQuery<A> partial;
	private FunctionalQuery<A> candidate;

	PartialFunctionTranslator(PartialFunction<A, C> partialFunction) {
		super(partialFunction);
		this.partialFunction = partialFunction;
	}

	public PartialFunction<A, C> getPartialFunction() {
		return partialFunction;
	}

	@Override
	public PartialFunctionTranslator<A, C> symbol(AnySymbol storageSymbol) {
		super.symbol(storageSymbol);
		return this;
	}

	@Override
	public <T> PartialFunctionTranslator<A, C> symbol(Symbol<T> storageSymbol,
													  StorageRefiner.Factory<T> storageRefiner) {
		super.symbol(storageSymbol, storageRefiner);
		return this;
	}

	public PartialFunctionTranslator<A, C> domain(Constraint domainRelation) {
		checkNotConfigured();
		if (this.domainConstraint != null) {
			throw new IllegalStateException("Domain was already set");
		}
		if (domainRelation.arity() != partialFunction.arity()) {
			throw new IllegalArgumentException("Arity of domain constraint %s doesn't match the partial function %s."
					.formatted(domainRelation, partialFunction));
		}
		for (var parameter : domainRelation.getParameters()) {
			if (parameter.getDirection() != ParameterDirection.OUT || !parameter.isNodeVariable()) {
				throw new IllegalArgumentException(
						"Domain constraint %s contains a parameter %s with an invalid direction or type".formatted(
								domainRelation, parameter));
			}
		}
		this.domainConstraint = domainRelation;
		return this;
	}

	@Override
	public PartialFunctionTranslator<A, C> interpretation(
			PartialInterpretation.Factory<A, C> interpretationFactory) {
		super.interpretation(interpretationFactory);
		return this;
	}

	@Override
	public PartialFunctionTranslator<A, C> refiner(
			PartialInterpretationRefiner.Factory<A, C> interpretationRefiner) {
		super.refiner(interpretationRefiner);
		return this;
	}

	public PartialFunctionTranslator<A, C> rewriter(PartialFunctionRewriter<A, C> rewriter) {
		checkNotConfigured();
		if (this.rewriter != null) {
			throw new IllegalArgumentException("Rewriter was already set");
		}
		this.rewriter = rewriter;
		return this;
	}

	public PartialFunctionTranslator<A, C> query(FunctionalQuery<A> query) {
		checkNotConfigured();
		if (this.query != null) {
			throw new IllegalArgumentException("Query was already set");
		}
		this.query = query;
		return this;
	}

	public PartialFunctionTranslator<A, C> partial(FunctionalQuery<A> partial) {
		checkNotConfigured();
		if (this.partial != null) {
			throw new IllegalArgumentException("Partial query was already set");
		}
		this.partial = partial;
		return this;
	}

	public PartialFunctionTranslator<A, C> candidate(FunctionalQuery<A> candidate) {
		checkNotConfigured();
		if (this.candidate != null) {
			throw new IllegalArgumentException("Candidate query was already set");
		}
		this.candidate = candidate;
		return this;
	}

	@Override
	protected void doConfigure(ModelStoreBuilder storeBuilder) {
		createFallbackQueryFromRewriter();
		liftQueries(storeBuilder);
		createFallbackQueriesFromSymbol();
		createFallbackRewriter();
		createFallbackInterpretation();
		createFallbackRefiner();
		createFallbackExclude(storeBuilder);
		createFallbackObjective();
		super.doConfigure(storeBuilder);
	}

	private void createFallbackQueryFromRewriter() {
		if (rewriter != null && query == null) {
			query = createFunctionalQuery(partialFunction.name(), (builder, parameters, output) -> builder
					.clause(
							ModalConstraint.of(Modality.MAY, domainConstraint).call(parameters),
							output.assign(partialFunction.call(parameters))
					));
		}
	}

	private void liftQueries(ModelStoreBuilder storeBuilder) {
		if (rewriter instanceof QueryBasedFunctionRewriter<A, C> queryBasedFunctionRewriter) {
			if (partial == null) {
				partial = queryBasedFunctionRewriter.getPartial();
			}
			if (candidate == null) {
				candidate = queryBasedFunctionRewriter.getCandidate();
			}
		} else if (query != null) {
			var reasoningBuilder = storeBuilder.getAdapter(ReasoningBuilder.class);
			if (partial == null) {
				partial = reasoningBuilder.lift(ModalitySpecification.UNSPECIFIED, ConcretenessSpecification.PARTIAL,
						query);
			}
			if (candidate == null) {
				candidate = reasoningBuilder.lift(ModalitySpecification.UNSPECIFIED,
                        ConcretenessSpecification.CANDIDATE, query);
			}
		}
	}

	private void createFallbackQueriesFromSymbol() {
		var type = partialFunction.abstractDomain().abstractType();
		if (domainConstraint == null || storageSymbol == null || storageSymbol.valueType() != type) {
			return;
		}
		// We just checked above that the type of the symbol is {@code A}.
		@SuppressWarnings("unchecked")
		var symbol = (Symbol<A>) storageSymbol;
		var symbolView = new FunctionView<>(symbol);
		var defaultValue = symbol.defaultValue();
		if (partial == null) {
			partial = createFunctionalQuery("partial", (builder, parameters, output) -> builder
					.clause(
							may(domainConstraint.call(parameters)),
							output.assign(symbolView.leftJoin(defaultValue, parameters))
					));
		}
		if (candidate == null) {
			candidate = createFunctionalQuery("candidate", (builder, parameters, output) -> builder
					.clause(
							candidateMay(domainConstraint.call(parameters)),
							output.assign(symbolView.leftJoin(defaultValue, parameters))
					));
		}
	}

	private RelationalQuery createRelationalQuery(String name, BiConsumer<QueryBuilder, NodeVariable[]> callback) {
		var queryBuilder = Query.builder(partialFunction.name() + "#" + name);
		var parameters = createParameters(queryBuilder);
		callback.accept(queryBuilder, parameters);
		return queryBuilder.build();
	}

	private NodeVariable[] createParameters(QueryBuilder queryBuilder) {
		int arity = partialFunction.arity();
		var parameters = new NodeVariable[arity];
		for (int i = 0; i < arity; i++) {
			parameters[i] = queryBuilder.parameter("p" + i);
		}
		return parameters;
	}

	private FunctionalQuery<A> createFunctionalQuery(String name, CreateQueryCallback<A> callback) {
		var queryBuilder = Query.builder(partialFunction.name() + "#" + name);
		var parameters = createParameters(queryBuilder);
		var output = Variable.of("output", partialFunction.abstractDomain().abstractType());
		var functionalQueryBuilder = queryBuilder.output(output);
		callback.accept(queryBuilder, parameters, output);
		return functionalQueryBuilder.build();
	}

	@FunctionalInterface
	private interface CreateQueryCallback<A> {
		void accept(QueryBuilder builder, NodeVariable[] parameters, DataVariable<A> output);
	}

	private void createFallbackInterpretation() {
		if (interpretationFactory == null && partial != null && candidate != null) {
			interpretationFactory = new QueryBasedFunctionInterpretationFactory<>(partial, candidate,
					partialFunction.abstractDomain());
		}
	}

	private void createFallbackRefiner() {
		if (interpretationRefiner == null && storageSymbol != null &&
				storageSymbol.valueType().equals(partialFunction.abstractDomain().abstractType())) {
			// We have just checked the value type of {@code storageSymbol}.
			@SuppressWarnings("unchecked")
			var symbol = (Symbol<A>) storageSymbol;
			interpretationRefiner = ConcreteSymbolRefiner.of(symbol);
		}
	}

	private void createFallbackRewriter() {
		if (rewriter == null && partial != null && candidate != null) {
			rewriter = new QueryBasedFunctionRewriter<>(partial, candidate, partialFunction.abstractDomain());
		}
	}

	private void createFallbackExclude(ModelStoreBuilder storeBuilder) {
		if (excludeWasSet || domainConstraint == null) {
			return;
		}
		var excludeQuery = createRelationalQuery("exclude", (builder, parameters) -> {
			var literals = new ArrayList<Literal>(parameters.length + 2);
			literals.add(must(domainConstraint.call(parameters)));
			literals.add(check(AbstractDomainTerms.isError(partialFunction.abstractDomain(),
					partialFunction.call(Concreteness.PARTIAL, parameters))));
			for (var parameter : parameters) {
				literals.add(must(ReasoningAdapter.EXISTS_SYMBOL.call(parameter)));
			}
			builder.clause(literals);
		});
		exclude = Criteria.whenHasMatch(excludeQuery);
		storeBuilder.tryGetAdapter(PropagationBuilder.class).ifPresent(this::configureFallbackExcludePropagator);
	}

	private void configureFallbackExcludePropagator(PropagationBuilder propagationBuilder) {
		var propagationRule = Rule.of(partialFunction.name() + "#excluded", this::configureFallbackExcludeRule);
		propagationBuilder.rule(propagationRule);
		if (domainConstraint instanceof PartialRelation domainRelation) {
			configureFallbackDomainExcludeRule(propagationBuilder, domainRelation);
			configureFallbackDomainExcludeConcretizationRule(propagationBuilder, domainRelation);
		}
	}

	private void configureFallbackExcludeRule(RuleBuilder builder, NodeVariable p1) {
		int arity = partialFunction.arity();
		for (int i = 0; i < arity; i++) {
			var parameters = new NodeVariable[arity];
			for (int j = 0; j < arity; j++) {
				parameters[j] = i == j ? p1 : Variable.of("v" + j);
			}
			var literals = new ArrayList<Literal>(arity + 3);
			literals.add(must(domainConstraint.call(parameters)));
			literals.add(check(AbstractDomainTerms.isError(partialFunction.abstractDomain(),
					partialFunction.call(Concreteness.PARTIAL, parameters))));
			for (int j = 0; j < arity; j++) {
				var parameter = parameters[j];
				if (i == j) {
					literals.add(may(ReasoningAdapter.EXISTS_SYMBOL.call(parameter)));
					literals.add(not(must(ReasoningAdapter.EXISTS_SYMBOL.call(parameter))));
				} else {
					literals.add(must(ReasoningAdapter.EXISTS_SYMBOL.call(parameter)));
				}
			}
			builder.clause(literals);
		}
		builder.action(PartialActionLiterals.remove(ReasoningAdapter.EXISTS_SYMBOL, p1));
	}

	private void configureFallbackDomainExcludeRule(PropagationBuilder propagationBuilder,
													PartialRelation domainRelation) {
		var builder = Rule.builder(partialFunction.name() + "#domainExcluded");
		int arity = partialFunction.arity();
		var parameters = new NodeVariable[arity];
		for (int i = 0; i < arity; i++) {
			parameters[i] = builder.parameter("p" + i);
		}
		var literals = new ArrayList<Literal>(parameters.length + 3);
		literals.add(may(domainRelation.call(parameters)));
		literals.add(not(must(domainRelation.call(parameters))));
		literals.add(check(AbstractDomainTerms.isError(partialFunction.abstractDomain(),
				partialFunction.call(Concreteness.PARTIAL, parameters))));
		for (var parameter : parameters) {
			literals.add(must(ReasoningAdapter.EXISTS_SYMBOL.call(parameter)));
		}
		builder.clause(literals);
		builder.action(PartialActionLiterals.remove(domainRelation, parameters));
		var domainPropagationRule = builder.build();
		propagationBuilder.rule(domainPropagationRule);
	}

	private void configureFallbackDomainExcludeConcretizationRule(PropagationBuilder propagationBuilder,
																  PartialRelation domainRelation) {
		var builder = Rule.builder(partialFunction.name() + "#concretizeDomainExcluded");
		int arity = partialFunction.arity();
		var parameters = new NodeVariable[arity];
		for (int i = 0; i < arity; i++) {
			parameters[i] = builder.parameter("p" + i);
		}
		var literals = new ArrayList<Literal>(parameters.length + 3);
		literals.add(candidateMay(domainRelation.call(parameters)));
		literals.add(not(candidateMust(domainRelation.call(parameters))));
		literals.add(check(AbstractDomainTerms.isError(partialFunction.abstractDomain(),
				partialFunction.call(Concreteness.CANDIDATE, parameters))));
		for (var parameter : parameters) {
			literals.add(candidateMust(ReasoningAdapter.EXISTS_SYMBOL.call(parameter)));
		}
		builder.clause(literals);
		builder.action(PartialActionLiterals.remove(domainRelation, parameters));
		var domainConcretizationRule = builder.build();
		propagationBuilder.concretizationRule(domainConcretizationRule);
	}

	private void createFallbackObjective() {
		if ((acceptWasSet && objectiveWasSet) || domainConstraint == null) {
			return;
		}
		var reject = createRelationalQuery("reject", (builder, parameters) -> {
			var literals = new ArrayList<Literal>(parameters.length + 2);
			literals.add(candidateMust(domainConstraint.call(parameters)));
			literals.add(check(AbstractDomainTerms.isError(partialFunction.abstractDomain(),
					partialFunction.call(Concreteness.CANDIDATE, parameters))));
			for (var parameter : parameters) {
				literals.add(candidateMust(ReasoningAdapter.EXISTS_SYMBOL.call(parameter)));
			}
			builder.clause(literals);
		});
		if (!acceptWasSet) {
			accept = Criteria.whenNoMatch(reject);
		}
		if (!objectiveWasSet) {
			objective = Objectives.count(reject);
		}
	}

	public PartialFunctionRewriter<A, C> getRewriter() {
		checkConfigured();
		return rewriter;
	}

	public static <A extends AbstractValue<A, C>, C> PartialFunctionTranslator<A, C> of(PartialFunction<A, C> partialFunction) {
		return new PartialFunctionTranslator<>(partialFunction);
	}
}
