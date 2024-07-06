/*
 * SPDX-FileCopyrightText: 2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning.translator.containment;

import tools.refinery.logic.literal.Literal;
import tools.refinery.logic.term.Variable;
import tools.refinery.store.dse.propagation.PropagationBuilder;
import tools.refinery.store.dse.transition.Rule;
import tools.refinery.store.model.ModelStoreBuilder;
import tools.refinery.store.model.ModelStoreConfiguration;
import tools.refinery.store.reasoning.representation.PartialRelation;
import tools.refinery.store.reasoning.translator.typehierarchy.TypeHierarchy;

import java.util.*;

import static tools.refinery.logic.literal.Literals.not;
import static tools.refinery.store.reasoning.actions.PartialActionLiterals.remove;
import static tools.refinery.store.reasoning.literal.PartialLiterals.may;
import static tools.refinery.store.reasoning.literal.PartialLiterals.must;
import static tools.refinery.store.reasoning.translator.containment.ContainmentHierarchyTranslator.*;

public class ContainerTypeInferenceTranslator implements ModelStoreConfiguration {
	private final TypeHierarchy typeHierarchy;
	private final Map<PartialRelation, Set<PartialRelation>> possibleContents = new LinkedHashMap<>();
	private final Map<PartialRelation, Set<PartialRelation>> possibleContainers = new LinkedHashMap<>();

	public ContainerTypeInferenceTranslator(TypeHierarchy typeHierarchy,
											Map<PartialRelation, ContainmentInfo> containmentInfoMap) {
		this.typeHierarchy = typeHierarchy;
		for (var containedType : typeHierarchy.getAnalysisResult(CONTAINER_SYMBOL).getConcreteSubtypesAndSelf()) {
			possibleContents.put(containedType, new LinkedHashSet<>());
		}
		for (var containedType : typeHierarchy.getAnalysisResult(CONTAINED_SYMBOL).getConcreteSubtypesAndSelf()) {
			possibleContainers.put(containedType, new LinkedHashSet<>());
		}
		for (var entry : containmentInfoMap.entrySet()) {
			var containmentLink = entry.getKey();
			var containmentInfo = entry.getValue();
			var sourceType = containmentInfo.sourceType();
			for (var subtype : typeHierarchy.getAnalysisResult(sourceType).getConcreteSubtypesAndSelf()) {
				possibleContents.get(subtype).add(containmentLink);
			}
			var targetType = containmentInfo.targetType();
			for (var subtype : typeHierarchy.getAnalysisResult(targetType).getConcreteSubtypesAndSelf()) {
				possibleContainers.get(subtype).add(containmentLink);
			}
		}
	}

	@Override
	public void apply(ModelStoreBuilder storeBuilder) {
		storeBuilder.tryGetAdapter(PropagationBuilder.class).ifPresent(this::configurePropagationBuilder);
	}

	private void configurePropagationBuilder(PropagationBuilder propagationBuilder) {
		configureContainerPropagator(propagationBuilder);
		configureContainedPropagator(propagationBuilder);
	}

	private void configureContainerPropagator(PropagationBuilder propagationBuilder) {
		for (var entry : possibleContents.entrySet()) {
			var sourceType = entry.getKey();
			var contents = entry.getValue();
			propagationBuilder.rule(Rule.of(sourceType.name() + "#notContainer", (builder, source) -> builder
					.clause(target -> {
						var literals = new ArrayList<Literal>();
						literals.add(may(sourceType.call(source)));
						literals.add(not(must(sourceType.call(source))));
						literals.add(must(CONTAINS_SYMBOL.call(source, target)));
						var typeInfo = typeHierarchy.getAnalysisResult(sourceType);
						for (var subtype : typeInfo.getDirectSubtypes()) {
							literals.add(not(may(subtype.call(source))));
						}
						for (var content : contents) {
							literals.add(not(may(content.call(source, target))));
						}
						return literals;
					})
					.action(
							remove(sourceType, source)
					)
			));
		}
	}

	private void configureContainedPropagator(PropagationBuilder propagationBuilder) {
		for (var entry : possibleContainers.entrySet()) {
			var targetType = entry.getKey();
			var containers = entry.getValue();
			propagationBuilder.rule(Rule.of(targetType.name() + "#notContent", (builder, target) -> builder
					.clause(() -> {
						var literals = new ArrayList<Literal>();
						literals.add(may(targetType.call(target)));
						literals.add(not(must(targetType.call(target))));
						literals.add(must(CONTAINED_SYMBOL.call(target)));
						var typeInfo = typeHierarchy.getAnalysisResult(targetType);
						for (var subtype : typeInfo.getDirectSubtypes()) {
							literals.add(not(may(subtype.call(target))));
						}
						for (var container : containers) {
							literals.add(not(may(container.call(Variable.of(), target))));
						}
						return literals;
					})
					.action(
							remove(targetType, target)
					)
			));
		}
	}
}
