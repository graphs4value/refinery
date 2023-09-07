/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning.translator;

import tools.refinery.store.dse.transition.Rule;
import tools.refinery.store.dse.transition.objectives.Criteria;
import tools.refinery.store.dse.transition.objectives.Criterion;
import tools.refinery.store.dse.transition.objectives.Objective;
import tools.refinery.store.dse.transition.objectives.Objectives;
import tools.refinery.store.model.ModelStoreBuilder;
import tools.refinery.store.query.Constraint;
import tools.refinery.store.query.dnf.Query;
import tools.refinery.store.query.dnf.QueryBuilder;
import tools.refinery.store.query.dnf.RelationalQuery;
import tools.refinery.store.query.literal.Literal;
import tools.refinery.store.query.term.NodeVariable;
import tools.refinery.store.query.view.MayView;
import tools.refinery.store.query.view.MustView;
import tools.refinery.store.reasoning.ReasoningAdapter;
import tools.refinery.store.reasoning.ReasoningBuilder;
import tools.refinery.store.reasoning.interpretation.PartialInterpretation;
import tools.refinery.store.reasoning.interpretation.PartialRelationRewriter;
import tools.refinery.store.reasoning.interpretation.QueryBasedRelationInterpretationFactory;
import tools.refinery.store.reasoning.interpretation.QueryBasedRelationRewriter;
import tools.refinery.store.reasoning.lifting.DnfLifter;
import tools.refinery.store.reasoning.literal.Concreteness;
import tools.refinery.store.reasoning.literal.Modality;
import tools.refinery.store.reasoning.literal.PartialLiterals;
import tools.refinery.store.reasoning.refinement.ConcreteSymbolRefiner;
import tools.refinery.store.reasoning.refinement.PartialInterpretationRefiner;
import tools.refinery.store.reasoning.refinement.PartialModelInitializer;
import tools.refinery.store.reasoning.refinement.StorageRefiner;
import tools.refinery.store.reasoning.representation.PartialRelation;
import tools.refinery.store.representation.AnySymbol;
import tools.refinery.store.representation.Symbol;
import tools.refinery.store.representation.TruthValue;

import java.util.ArrayList;
import java.util.function.BiConsumer;

import static tools.refinery.store.query.literal.Literals.not;

@SuppressWarnings("UnusedReturnValue")
public final class PartialRelationTranslator extends PartialSymbolTranslator<TruthValue, Boolean> {
	private final PartialRelation partialRelation;
	private PartialRelationRewriter rewriter;
	private RelationalQuery query;
	private RelationalQuery may;
	private RelationalQuery must;
	private RelationalQuery candidateMay;
	private RelationalQuery candidateMust;
	private RoundingMode roundingMode;

	private PartialRelationTranslator(PartialRelation partialRelation) {
		super(partialRelation);
		this.partialRelation = partialRelation;
	}

	public PartialRelation getPartialRelation() {
		return partialRelation;
	}

	@Override
	public PartialRelationTranslator symbol(AnySymbol storageSymbol) {
		super.symbol(storageSymbol);
		return this;
	}

	@Override
	public <T> PartialRelationTranslator symbol(Symbol<T> storageSymbol,
												StorageRefiner.Factory<T> storageRefiner) {
		super.symbol(storageSymbol, storageRefiner);
		return this;
	}

	@Override
	public PartialRelationTranslator interpretation(
			PartialInterpretation.Factory<TruthValue, Boolean> interpretationFactory) {
		super.interpretation(interpretationFactory);
		return this;
	}

	@Override
	public PartialRelationTranslator refiner(
			PartialInterpretationRefiner.Factory<TruthValue, Boolean> interpretationRefiner) {
		super.refiner(interpretationRefiner);
		return this;
	}

	public PartialRelationTranslator rewriter(PartialRelationRewriter rewriter) {
		checkNotConfigured();
		if (this.rewriter != null) {
			throw new IllegalArgumentException("Rewriter was already set");
		}
		this.rewriter = rewriter;
		return this;
	}

	@Override
	public PartialRelationTranslator initializer(PartialModelInitializer initializer) {
		super.initializer(initializer);
		return this;
	}

	@Override
	public PartialRelationTranslator decision(Rule decisionRule) {
		super.decision(decisionRule);
		return this;
	}

	@Override
	public PartialRelationTranslator accept(Criterion acceptanceCriterion) {
		super.accept(acceptanceCriterion);
		return this;
	}

	@Override
	public PartialRelationTranslator exclude(Criterion exclusionCriterion) {
		super.exclude(exclusionCriterion);
		return this;
	}

	@Override
	public PartialRelationTranslator objective(Objective objective) {
		super.objective(objective);
		return this;
	}

	public PartialRelationTranslator query(RelationalQuery query) {
		checkNotConfigured();
		if (this.query != null) {
			throw new IllegalArgumentException("Query was already set");
		}
		this.query = query;
		return this;
	}

	public PartialRelationTranslator may(RelationalQuery may) {
		checkNotConfigured();
		if (this.may != null) {
			throw new IllegalArgumentException("May query was already set");
		}
		this.may = may;
		return this;
	}

	public PartialRelationTranslator mayNever() {
		var never = createQuery(partialRelation.name() + "#never", (builder, parameters) -> {});
		may(never);
		return this;
	}

	public PartialRelationTranslator must(RelationalQuery must) {
		checkNotConfigured();
		if (this.must != null) {
			throw new IllegalArgumentException("Must query was already set");
		}
		this.must = must;
		return this;
	}

	public PartialRelationTranslator candidate(RelationalQuery candidate) {
		candidateMay(candidate);
		candidateMust(candidate);
		return this;
	}

	public PartialRelationTranslator candidateMay(RelationalQuery candidateMay) {
		checkNotConfigured();
		if (this.candidateMay != null) {
			throw new IllegalArgumentException("Candidate may query was already set");
		}
		this.candidateMay = candidateMay;
		return this;
	}

	public PartialRelationTranslator candidateMust(RelationalQuery candidateMust) {
		checkNotConfigured();
		if (this.candidateMust != null) {
			throw new IllegalArgumentException("Candidate must query was already set");
		}
		this.candidateMust = candidateMust;
		return this;
	}

	public PartialRelationTranslator roundingMode(RoundingMode roundingMode) {
		checkNotConfigured();
		if (this.roundingMode != null) {
			throw new IllegalArgumentException("Rounding mode was already set");
		}
		this.roundingMode = roundingMode;
		return this;
	}

	@Override
	protected void doConfigure(ModelStoreBuilder storeBuilder) {
		setFallbackRoundingMode();
		createFallbackQueryFromRewriter();
		liftQueries(storeBuilder);
		createFallbackQueriesFromSymbol();
		setFallbackCandidateQueries();
		createFallbackRewriter();
		createFallbackInterpretation();
		createFallbackRefiner();
		createFallbackExclude();
		createFallbackObjective();
		super.doConfigure(storeBuilder);
	}

	private void setFallbackRoundingMode() {
		if (roundingMode == null) {
			roundingMode = query == null && storageSymbol != null ? RoundingMode.PREFER_FALSE : RoundingMode.NONE;
		}
	}

	private RelationalQuery createQuery(String name, BiConsumer<QueryBuilder, NodeVariable[]> callback) {
		int arity = partialRelation.arity();
		var queryBuilder = Query.builder(name);
		var parameters = new NodeVariable[arity];
		for (int i = 0; i < arity; i++) {
			parameters[i] = queryBuilder.parameter("p" + 1);
		}
		callback.accept(queryBuilder, parameters);
		return queryBuilder.build();
	}

	private RelationalQuery createQuery(String name, Constraint constraint) {
		return createQuery(name, (builder, parameters) -> builder.clause(constraint.call(parameters)));
	}

	private void createFallbackQueryFromRewriter() {
		if (rewriter != null && query == null) {
			query = createQuery(partialRelation.name(), partialRelation);
		}
	}

	private void createFallbackQueriesFromSymbol() {
		if (storageSymbol == null || storageSymbol.valueType() != TruthValue.class) {
			return;
		}
		// We checked in the guard clause that this is safe.
		@SuppressWarnings("unchecked")
		var typedStorageSymbol = (Symbol<TruthValue>) storageSymbol;
		var defaultValue = typedStorageSymbol.defaultValue();
		if (may == null && !defaultValue.may()) {
			may = createQuery(DnfLifter.decorateName(partialRelation.name(), Modality.MAY, Concreteness.PARTIAL),
					new MayView(typedStorageSymbol));
		}
		if (must == null && !defaultValue.must()) {
			must = createQuery(DnfLifter.decorateName(partialRelation.name(), Modality.MUST, Concreteness.PARTIAL),
					new MustView(typedStorageSymbol));
		}
	}

	private void liftQueries(ModelStoreBuilder storeBuilder) {
		if (rewriter instanceof QueryBasedRelationRewriter queryBasedRelationRewriter) {
			liftQueriesFromQueryBasedRewriter(queryBasedRelationRewriter);
		} else if (query != null) {
			liftQueriesFromFourValuedQuery(storeBuilder);
		}
	}

	private void liftQueriesFromQueryBasedRewriter(QueryBasedRelationRewriter queryBasedRelationRewriter) {
		if (may == null) {
			may = queryBasedRelationRewriter.getMay();
		}
		if (must == null) {
			must = queryBasedRelationRewriter.getMust();
		}
		if (candidateMay == null) {
			candidateMay = queryBasedRelationRewriter.getCandidateMay();
		}
		if (candidateMust == null) {
			candidateMust = queryBasedRelationRewriter.getCandidateMust();
		}
	}

	private void liftQueriesFromFourValuedQuery(ModelStoreBuilder storeBuilder) {
		var reasoningBuilder = storeBuilder.getAdapter(ReasoningBuilder.class);
		if (may == null) {
			may = reasoningBuilder.lift(Modality.MAY, Concreteness.PARTIAL, query);
		}
		if (must == null) {
			must = reasoningBuilder.lift(Modality.MUST, Concreteness.PARTIAL, query);
		}
		if (candidateMay == null) {
			candidateMay = reasoningBuilder.lift(Modality.MAY, Concreteness.CANDIDATE, query);
		}
		if (candidateMust == null) {
			candidateMust = reasoningBuilder.lift(Modality.MAY, Concreteness.CANDIDATE, query);
		}
	}

	private void setFallbackCandidateQueries() {
		if (candidateMay == null) {
			candidateMay = switch (roundingMode) {
				case NONE, PREFER_TRUE -> may;
				case PREFER_FALSE -> must;
			};
		}
		if (candidateMust == null) {
			candidateMust = switch (roundingMode) {
				case NONE, PREFER_FALSE -> must;
				case PREFER_TRUE -> may;
			};
		}
	}

	private void createFallbackRewriter() {
		if (rewriter == null) {
			rewriter = new QueryBasedRelationRewriter(may, must, candidateMay, candidateMust);
		}
	}

	private void createFallbackInterpretation() {
		if (interpretationFactory == null) {
			interpretationFactory = new QueryBasedRelationInterpretationFactory(may, must, candidateMay, candidateMust);
		}
	}

	private void createFallbackRefiner() {
		if (interpretationRefiner == null && storageSymbol != null && storageSymbol.valueType() == TruthValue.class) {
			// We checked in the condition that this is safe.
			@SuppressWarnings("unchecked")
			var typedStorageSymbol = (Symbol<TruthValue>) storageSymbol;
			interpretationRefiner = ConcreteSymbolRefiner.of(typedStorageSymbol);
		}
	}

	private void createFallbackExclude() {
		if (excludeWasSet) {
			return;
		}
		var excludeQuery = createQuery("exclude", (builder, parameters) -> {
			var literals = new ArrayList<Literal>(parameters.length + 2);
			literals.add(PartialLiterals.must(partialRelation.call(parameters)));
			literals.add(not(PartialLiterals.may(partialRelation.call(parameters))));
			for (var parameter : parameters) {
				literals.add(PartialLiterals.must(ReasoningAdapter.EXISTS_SYMBOL.call(parameter)));
			}
			builder.clause(literals);
		});
		exclude = Criteria.whenHasMatch(excludeQuery);
	}

	private void createFallbackObjective() {
		if (acceptWasSet && objectiveWasSet) {
			return;
		}
		var invalidCandidate = createQuery("invalidCandidate", (builder, parameters) -> builder
				.clause(
						PartialLiterals.candidateMust(partialRelation.call(parameters)),
						not(PartialLiterals.candidateMay(partialRelation.call(parameters)))
				)
				.clause(
						PartialLiterals.candidateMust(partialRelation.call(parameters)),
						not(PartialLiterals.may(partialRelation.call(parameters)))
				)
				.clause(
						PartialLiterals.must(partialRelation.call(parameters)),
						not(PartialLiterals.candidateMay(partialRelation.call(parameters)))
				));
		var reject = createQuery("reject", (builder, parameters) -> {
			var literals = new ArrayList<Literal>(parameters.length + 1);
			literals.add(invalidCandidate.call(parameters));
			for (var parameter : parameters) {
				literals.add(PartialLiterals.candidateMust(ReasoningAdapter.EXISTS_SYMBOL.call(parameter)));
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

	public PartialRelationRewriter getRewriter() {
		checkConfigured();
		return rewriter;
	}

	public static PartialRelationTranslator of(PartialRelation relation) {
		return new PartialRelationTranslator(relation);
	}
}
