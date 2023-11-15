/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning.translator.containment;

import tools.refinery.store.model.Interpretation;
import tools.refinery.store.reasoning.ReasoningAdapter;
import tools.refinery.store.reasoning.refinement.AbstractPartialInterpretationRefiner;
import tools.refinery.store.reasoning.refinement.PartialInterpretationRefiner;
import tools.refinery.store.reasoning.representation.PartialRelation;
import tools.refinery.store.reasoning.representation.PartialSymbol;
import tools.refinery.store.representation.Symbol;
import tools.refinery.store.representation.TruthValue;
import tools.refinery.store.tuple.Tuple;

import java.util.ArrayList;
import java.util.Set;

class ContainmentLinkRefiner extends AbstractPartialInterpretationRefiner<TruthValue, Boolean> {
	private final Factory factory;
	private final Interpretation<InferredContainment> interpretation;
	private final PartialInterpretationRefiner<TruthValue, Boolean> sourceRefiner;
	private final PartialInterpretationRefiner<TruthValue, Boolean> targetRefiner;

	private ContainmentLinkRefiner(ReasoningAdapter adapter, PartialSymbol<TruthValue, Boolean> partialSymbol,
								   Factory factory) {
		super(adapter, partialSymbol);
		this.factory = factory;
		interpretation = adapter.getModel().getInterpretation(factory.symbol);
		sourceRefiner = adapter.getRefiner(factory.sourceType);
		targetRefiner = adapter.getRefiner(factory.targetType);
	}

	@Override
	public boolean merge(Tuple key, TruthValue value) {
		var oldValue = interpretation.get(key);
		var newValue = mergeLink(oldValue, value);
		if (oldValue != newValue) {
			interpretation.put(key, newValue);
		}
		if (value.must()) {
			return sourceRefiner.merge(Tuple.of(key.get(0)), TruthValue.TRUE) &&
					targetRefiner.merge(Tuple.of(key.get(1)), TruthValue.TRUE);
		}
		return true;
	}

	public InferredContainment mergeLink(InferredContainment oldValue, TruthValue toMerge) {
		return switch (toMerge) {
			case UNKNOWN -> oldValue;
			case TRUE -> mustLink(oldValue);
			case FALSE -> forbidLink(oldValue);
			case ERROR -> errorLink(oldValue);
		};
	}

	public InferredContainment mustLink(InferredContainment oldValue) {
		var mustLinks = oldValue.mustLinks();
		if (oldValue.contains().may() && mustLinks.isEmpty() && oldValue.forbiddenLinks().isEmpty()) {
			return factory.trueLink;
		}
		if (mustLinks.contains(factory.linkType)) {
			return oldValue;
		}
		return new InferredContainment(oldValue.contains().merge(TruthValue.TRUE),
				addToSet(mustLinks, factory.linkType), oldValue.forbiddenLinks());
	}

	public InferredContainment forbidLink(InferredContainment oldValue) {
		var forbiddenLinks = oldValue.forbiddenLinks();
		if (oldValue.contains() == TruthValue.UNKNOWN && oldValue.mustLinks().isEmpty() && forbiddenLinks.isEmpty()) {
			return factory.falseLinkUnknownContains;
		}
		if (forbiddenLinks.contains(factory.linkType)) {
			return oldValue;
		}
		return new InferredContainment(oldValue.contains(), oldValue.mustLinks(),
				addToSet(forbiddenLinks, factory.linkType));
	}

	public InferredContainment errorLink(InferredContainment oldValue) {
		return new InferredContainment(TruthValue.ERROR, addToSet(oldValue.mustLinks(), factory.linkType),
				addToSet(oldValue.forbiddenLinks(), factory.linkType));
	}

	private static Set<PartialRelation> addToSet(Set<PartialRelation> oldSet, PartialRelation linkType) {
		if (oldSet.isEmpty()) {
			return Set.of(linkType);
		}
		var newElements = new ArrayList<PartialRelation>(oldSet.size() + 1);
		newElements.addAll(oldSet);
		newElements.add(linkType);
		return Set.copyOf(newElements);
	}

	public static PartialInterpretationRefiner.Factory<TruthValue, Boolean> of(
			PartialRelation linkType, Symbol<InferredContainment> symbol, PartialRelation sourceType,
			PartialRelation targetType) {
		return new Factory(linkType, symbol, sourceType, targetType);
	}

	private static class Factory implements PartialInterpretationRefiner.Factory<TruthValue, Boolean> {
		public final PartialRelation linkType;
		public final Symbol<InferredContainment> symbol;
		public final PartialRelation targetType;
		public final PartialRelation sourceType;
		public final InferredContainment trueLink;
		public final InferredContainment falseLinkUnknownContains;

		public Factory(PartialRelation linkType, Symbol<InferredContainment> symbol, PartialRelation sourceType,
					   PartialRelation targetType) {
			this.linkType = linkType;
			this.symbol = symbol;
			this.sourceType = sourceType;
			this.targetType = targetType;
			trueLink = new InferredContainment(TruthValue.TRUE, Set.of(linkType), Set.of());
			falseLinkUnknownContains = new InferredContainment(TruthValue.FALSE, Set.of(), Set.of(linkType));
		}

		@Override
		public PartialInterpretationRefiner<TruthValue, Boolean> create(
				ReasoningAdapter adapter, PartialSymbol<TruthValue, Boolean> partialSymbol) {
			return new ContainmentLinkRefiner(adapter, partialSymbol, this);
		}
	}
}
