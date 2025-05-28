package tools.refinery.store.reasoning.translator.attribute;

import tools.refinery.logic.AbstractValue;
import tools.refinery.logic.dnf.Query;
import tools.refinery.logic.term.abstractdomain.AbstractDomainTerms;
import tools.refinery.store.dse.propagation.PropagationBuilder;
import tools.refinery.store.dse.transition.Rule;
import tools.refinery.store.dse.transition.objectives.Criteria;
import tools.refinery.store.model.ModelStoreBuilder;
import tools.refinery.store.model.ModelStoreConfiguration;
import tools.refinery.store.query.view.FunctionView;
import tools.refinery.store.reasoning.ReasoningAdapter;
import tools.refinery.store.reasoning.literal.Concreteness;
import tools.refinery.store.reasoning.representation.PartialFunction;
import tools.refinery.store.reasoning.representation.PartialRelation;
import tools.refinery.store.reasoning.translator.PartialFunctionTranslator;
import tools.refinery.store.representation.Symbol;

import static tools.refinery.logic.literal.Literals.check;
import static tools.refinery.store.reasoning.actions.PartialActionLiterals.remove;
import static tools.refinery.store.reasoning.literal.PartialLiterals.*;

public class AttributeTranslator<A extends AbstractValue<A, C>, C> implements ModelStoreConfiguration {
	private final PartialRelation partialRelation;
	private final PartialFunction<A, C> partialFunction;

	public AttributeTranslator(PartialFunction<A, C> partialFunction, AttributeInfo attributeInfo) {
		this.partialFunction = partialFunction;
		this.partialRelation = attributeInfo.owningType();
	}

	@Override
	public void apply(ModelStoreBuilder storeBuilder) {
		PartialFunctionTranslator<A, C> translator = PartialFunctionTranslator.of(partialFunction);
		Symbol<A> symbol = new Symbol<>(partialFunction.name(), 1, partialFunction.abstractDomain().abstractType(),
				partialFunction.abstractDomain().unknown());
		translator.symbol(symbol);

		var functionView = new FunctionView<>(symbol);

		translator.partial(
				Query.of(partialFunction.name() + "#partial", partialFunction.abstractDomain().abstractType(),
						(builder, p1, output) -> builder.clause(
								may(partialRelation.call(p1)),
								output.assign(functionView.leftJoin(partialFunction.abstractDomain().unknown(), p1))
						))
		);

		translator.candidate(
				Query.of(partialFunction.name() + "#candidate", partialFunction.abstractDomain().abstractType(),
						(builder, p1, output) -> builder.clause(
								candidateMay(partialRelation.call(p1)),
								output.assign(functionView.leftJoin(partialFunction.abstractDomain().unknown(),p1))
						))
		);

		translator.accept(Criteria.whenNoMatch(
				Query.of(partialFunction.name() + "#accept", (builder, p1) -> builder.clause(
						candidateMust(partialRelation.call(p1)),
						candidateMust(ReasoningAdapter.EXISTS_SYMBOL.call(p1)),
						check(AbstractDomainTerms.isError(partialFunction.abstractDomain(),
								partialFunction.call(Concreteness.CANDIDATE, p1)))
				))
		));

		translator.exclude(Criteria.whenHasMatch(
				Query.of(partialFunction.name() + "#exclude", (builder, p1) -> builder.clause(
						must(partialRelation.call(p1)),
						must(ReasoningAdapter.EXISTS_SYMBOL.call(p1)),
						check(AbstractDomainTerms.isError(partialFunction.abstractDomain(),
								partialFunction.call(Concreteness.PARTIAL, p1)))
				))
		));

		storeBuilder.with(translator);

		storeBuilder.tryGetAdapter(PropagationBuilder.class).ifPresent(propagationBuilder -> {
			propagationBuilder.rule(Rule.of(partialFunction.name() + "#notDefinedAt", (builder, p1) -> builder
					.clause(
							may(partialRelation.call(p1)),
							check(AbstractDomainTerms.isError(partialFunction.abstractDomain(),
									partialFunction.call(Concreteness.PARTIAL, p1)))
					)
					.action(
							remove(partialRelation, p1)
					)));

			propagationBuilder.concretizationRule(Rule.of(partialFunction.name() + "#concretizeNotDefinedAt",
					(builder, p1) -> builder
							.clause(
									candidateMay(partialRelation.call(p1)),
									check(AbstractDomainTerms.isError(partialFunction.abstractDomain(),
											partialFunction.call(Concreteness.CANDIDATE, p1)))
							)
							.action(
									remove(partialRelation, p1)
							)));
		});
	}
}
