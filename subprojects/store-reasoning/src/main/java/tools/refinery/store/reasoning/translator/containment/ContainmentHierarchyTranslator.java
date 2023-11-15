/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning.translator.containment;

import tools.refinery.store.dse.transition.DesignSpaceExplorationBuilder;
import tools.refinery.store.dse.transition.Rule;
import tools.refinery.store.model.ModelStoreBuilder;
import tools.refinery.store.model.ModelStoreConfiguration;
import tools.refinery.store.query.dnf.Query;
import tools.refinery.store.query.dnf.RelationalQuery;
import tools.refinery.store.query.literal.Connectivity;
import tools.refinery.store.query.literal.Literal;
import tools.refinery.store.query.literal.RepresentativeElectionLiteral;
import tools.refinery.store.query.term.Variable;
import tools.refinery.store.query.view.AnySymbolView;
import tools.refinery.store.reasoning.lifting.DnfLifter;
import tools.refinery.store.reasoning.literal.Concreteness;
import tools.refinery.store.reasoning.literal.CountLowerBoundLiteral;
import tools.refinery.store.reasoning.literal.Modality;
import tools.refinery.store.reasoning.refinement.RefinementBasedInitializer;
import tools.refinery.store.reasoning.representation.PartialRelation;
import tools.refinery.store.reasoning.translator.PartialRelationTranslator;
import tools.refinery.store.reasoning.translator.RoundingMode;
import tools.refinery.store.reasoning.translator.multiobject.MultiObjectTranslator;
import tools.refinery.store.reasoning.translator.multiplicity.ConstrainedMultiplicity;
import tools.refinery.store.reasoning.translator.multiplicity.InvalidMultiplicityErrorTranslator;
import tools.refinery.store.representation.Symbol;
import tools.refinery.store.representation.cardinality.CardinalityIntervals;
import tools.refinery.store.representation.cardinality.FiniteUpperCardinality;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static tools.refinery.store.query.literal.Literals.check;
import static tools.refinery.store.query.literal.Literals.not;
import static tools.refinery.store.query.term.int_.IntTerms.constant;
import static tools.refinery.store.query.term.int_.IntTerms.less;
import static tools.refinery.store.reasoning.ReasoningAdapter.EXISTS_SYMBOL;
import static tools.refinery.store.reasoning.actions.PartialActionLiterals.add;
import static tools.refinery.store.reasoning.actions.PartialActionLiterals.focus;
import static tools.refinery.store.reasoning.literal.PartialLiterals.*;

public class ContainmentHierarchyTranslator implements ModelStoreConfiguration {
	public static final PartialRelation CONTAINED_SYMBOL = new PartialRelation("contained", 1);
	public static final PartialRelation INVALID_CONTAINER = new PartialRelation("invalidContainer",
			1);
	public static final PartialRelation CONTAINS_SYMBOL = new PartialRelation("contains", 2);

	private final Symbol<InferredContainment> containsStorage = Symbol.of("CONTAINS", 2, InferredContainment.class,
			InferredContainment.UNKNOWN);
	private final AnySymbolView mustAnyContainmentLinkView = new MustAnyContainmentLinkView(containsStorage);
	private final AnySymbolView forbiddenContainsView = new ForbiddenContainsView(containsStorage);
	private final RelationalQuery containsMayNewTargetHelper;
	private final RelationalQuery containsWithoutLink;
	private final RelationalQuery weakComponents;
	private final RelationalQuery strongComponents;
	private final Map<PartialRelation, ContainmentInfo> containmentInfoMap;

	public ContainmentHierarchyTranslator(Map<PartialRelation, ContainmentInfo> containmentInfoMap) {
		this.containmentInfoMap = containmentInfoMap;

		var name = CONTAINS_SYMBOL.name();

		containsMayNewTargetHelper = Query.of(name + "#mayNewTargetHelper", (builder, child) -> builder
				.clause(Integer.class, existingContainers -> List.of(
						may(CONTAINED_SYMBOL.call(child)),
						new CountLowerBoundLiteral(existingContainers, CONTAINS_SYMBOL,
								List.of(Variable.of(), child)),
						check(less(existingContainers, constant(1)))
				)));

		containsWithoutLink = Query.of(name + "#withoutLink", (builder, parent, child) -> builder.clause(
				must(CONTAINS_SYMBOL.call(parent, child)),
				not(mustAnyContainmentLinkView.call(parent, child))
		));

		var mustExistBothContains = Query.of(name + "#mustExistBoth", (builder, parent, child) -> builder.clause(
				must(CONTAINS_SYMBOL.call(parent, child)),
				must(EXISTS_SYMBOL.call(parent)),
				must(EXISTS_SYMBOL.call(child))
		));

		weakComponents = Query.of(name + "#weakComponents", (builder, node, representative) -> builder.clause(
				new RepresentativeElectionLiteral(Connectivity.WEAK, mustExistBothContains.getDnf(), node,
						representative)
		));

		strongComponents = Query.of(name + "#strongComponents", (builder, node, representative) -> builder.clause(
				new RepresentativeElectionLiteral(Connectivity.STRONG, mustExistBothContains.getDnf(), node,
						representative)
		));
	}

	@Override
	public void apply(ModelStoreBuilder storeBuilder) {
		storeBuilder.symbol(containsStorage);
		translateContains(storeBuilder);
		translateInvalidContainer(storeBuilder);
		for (var entry : containmentInfoMap.entrySet()) {
			var linkType = entry.getKey();
			var info = entry.getValue();
			translateContainmentLinkType(storeBuilder, linkType, info);
			translateInvalidMultiplicity(storeBuilder, linkType, info);
		}
		translateFocusNotContained(storeBuilder);
	}

	private void translateContainmentLinkType(ModelStoreBuilder storeBuilder, PartialRelation linkType,
											  ContainmentInfo info) {
		var name = linkType.name();
		var sourceType = info.sourceType();
		var targetType = info.targetType();
		var upperCardinality = info.multiplicity().multiplicity().upperBound();

		var mayNewSourceHelper = Query.of(name + "#mayNewSourceHelper", (builder, parent) -> {
			var literals = new ArrayList<Literal>();
			literals.add(may(sourceType.call(parent)));
			if (upperCardinality instanceof FiniteUpperCardinality finiteUpperCardinality) {
				var existingCount = Variable.of("existingCount", Integer.class);
				literals.add(new CountLowerBoundLiteral(existingCount, linkType, List.of(parent, Variable.of())));
				literals.add(check(less(existingCount, constant(finiteUpperCardinality.finiteUpperBound()))));
			}
			builder.clause(literals);
		});

		var mayNewTargetHelper = Query.of(name + "#mayNewTargetHelper", (builder, child) -> builder.clause(
				containsMayNewTargetHelper.call(child),
				may(targetType.call(child))
		));

		var forbiddenLinkView = new ForbiddenContainmentLinkView(containsStorage, linkType);

		var mayNewHelper = Query.of(name + "#mayNewHelper", (builder, parent, child) -> builder.clause(
				mayNewSourceHelper.call(parent),
				mayNewTargetHelper.call(child),
				not(mustAnyContainmentLinkView.call(parent, child)),
				not(forbiddenLinkView.call(parent, child))
		));

		var existingContainsLink = Query.of(name + "#existingContaints", (builder, parent, child) -> builder
				.clause(
						must(linkType.call(parent, child))
				)
				.clause(
						containsWithoutLink.call(parent, child)
				));

		var mayExistingHelper = Query.of(name + "#mayExistingHelper", (builder, parent, child) -> builder.clause(
				existingContainsLink.call(parent, child),
				not(forbiddenContainsView.call(parent, child)),
				may(sourceType.call(parent)),
				may(targetType.call(child)),
				not(forbiddenLinkView.call(parent, child))
				// Violation of monotonicity:
				// Containment edges violating upper multiplicity will not be marked as {@code ERROR}, but the
				// {@code invalidNumberOfContainers} error pattern will already mark the node as invalid.
		));

		var may = Query.of(name + "#may", (builder, parent, child) -> builder
				.clause(
						mayNewHelper.call(parent, child),
						not(weakComponents.call(parent, Variable.of()))
				)
				.clause(representative -> List.of(
						mayNewHelper.call(parent, child),
						weakComponents.call(parent, representative),
						// Violation of para-consistency:
						// If there is a surely existing node with at least two containers, its (transitive) containers
						// will end up in the same weakly connected component, and we will spuriously mark the
						// containment edge between the (transitive) containers as {@code FALSE}. However, such
						// models can never be finished.
						//
						// Violation of monotonicity:
						// if the a {@code TRUE} value is added to the representation in the previous situation,
						// we'll check strongly connected components instead of weakly connected ones. Therefore, the
						// view for the partial symbol will change from {@code FALSE} to {@code TRUE}. This doesn't
						// affect the overall inconsistency of the partial model (due to the surely existing node
						// with multiple containers).
						not(weakComponents.call(child, representative))
				))
				.clause(
						mayExistingHelper.call(parent, child),
						not(strongComponents.call(parent, Variable.of()))
				)
				.clause(representative -> List.of(
						mayExistingHelper.call(parent, child),
						strongComponents.call(parent, representative),
						not(strongComponents.call(child, representative))
				)));

		storeBuilder.with(PartialRelationTranslator.of(linkType)
				.may(may)
				.must(Query.of(name + "#must", (builder, parent, child) -> builder.clause(
						new MustContainmentLinkView(containsStorage, linkType).call(parent, child)
				)))
				.roundingMode(RoundingMode.PREFER_FALSE)
				.refiner(ContainmentLinkRefiner.of(linkType, containsStorage, info.sourceType(), info.targetType()))
				.initializer(new RefinementBasedInitializer<>(linkType))
				.decision(Rule.of(linkType.name(), (builder, source, target) -> builder
						.clause(
								may(linkType.call(source, target)),
								not(candidateMust(linkType.call(source, target))),
								not(MultiObjectTranslator.MULTI_VIEW.call(source))
						)
						.action(focusedTarget -> List.of(
								focus(target, focusedTarget),
								add(linkType, source, focusedTarget)
						)))));
	}

	private void translateInvalidMultiplicity(ModelStoreBuilder storeBuilder, PartialRelation linkType,
											  ContainmentInfo info) {
		storeBuilder.with(new InvalidMultiplicityErrorTranslator(info.sourceType(), linkType, false,
				info.multiplicity()));
	}

	private void translateContains(ModelStoreBuilder storeBuilder) {
		var name = CONTAINS_SYMBOL.name();
		var mustName = DnfLifter.decorateName(name, Modality.MUST, Concreteness.PARTIAL);

		storeBuilder.with(PartialRelationTranslator.of(CONTAINS_SYMBOL)
				.query(Query.of(name, (builder, parent, child) -> {
					for (var linkType : containmentInfoMap.keySet()) {
						builder.clause(linkType.call(parent, child));
					}
				}))
				.must(Query.of(mustName, (builder, parent, child) -> builder.clause(
						new MustContainsView(containsStorage).call(parent, child)
				)))
				.refiner(ContainsRefiner.of(containsStorage))
				.initializer(new RefinementBasedInitializer<>(CONTAINS_SYMBOL)));
	}

	private void translateInvalidContainer(ModelStoreBuilder storeBuilder) {
		storeBuilder.with(new InvalidMultiplicityErrorTranslator(CONTAINED_SYMBOL, CONTAINS_SYMBOL, true,
				ConstrainedMultiplicity.of(CardinalityIntervals.ONE, INVALID_CONTAINER)));
	}

	private void translateFocusNotContained(ModelStoreBuilder storeBuilder) {
		var dseBuilderOption = storeBuilder.tryGetAdapter(DesignSpaceExplorationBuilder.class);
		if (dseBuilderOption.isEmpty()) {
			return;
		}
		var dseBuilder = dseBuilderOption.get();
		dseBuilder.transformation(Rule.of("NOT_CONTAINED", (builder, multi) -> builder
				.clause(
						MultiObjectTranslator.MULTI_VIEW.call(multi),
						not(may(CONTAINED_SYMBOL.call(multi)))
				)
				.clause(container -> List.of(
						MultiObjectTranslator.MULTI_VIEW.call(multi),
						mustAnyContainmentLinkView.call(container, multi),
						not(MultiObjectTranslator.MULTI_VIEW.call(container))
				))
				.action(
						focus(multi, Variable.of())
				)));
	}
}
