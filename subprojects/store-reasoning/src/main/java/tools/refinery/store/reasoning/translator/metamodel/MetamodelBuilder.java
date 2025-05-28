/*
 * SPDX-FileCopyrightText: 2023-2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning.translator.metamodel;

import tools.refinery.store.reasoning.representation.AnyPartialFunction;
import tools.refinery.store.reasoning.representation.PartialRelation;
import tools.refinery.store.reasoning.translator.TranslationException;
import tools.refinery.store.reasoning.translator.attribute.AttributeInfo;
import tools.refinery.store.reasoning.translator.containment.ContainmentHierarchyTranslator;
import tools.refinery.store.reasoning.translator.containment.ContainmentInfo;
import tools.refinery.store.reasoning.translator.crossreference.DirectedCrossReferenceInfo;
import tools.refinery.store.reasoning.translator.crossreference.UndirectedCrossReferenceInfo;
import tools.refinery.store.reasoning.translator.multiplicity.Multiplicity;
import tools.refinery.store.reasoning.translator.multiplicity.UnconstrainedMultiplicity;
import tools.refinery.store.reasoning.translator.typehierarchy.TypeInfo;

import java.util.*;
import java.util.function.Consumer;

public class MetamodelBuilder {
	private final ContainedTypeHierarchyBuilder typeHierarchyBuilder = new ContainedTypeHierarchyBuilder();
	private final Map<PartialRelation, ReferenceInfo> referenceInfoMap = new LinkedHashMap<>();
	private final Set<PartialRelation> containerTypes = new HashSet<>();
	private final Set<PartialRelation> containedTypes = new HashSet<>();
	private final Map<PartialRelation, ContainmentInfo> containmentHierarchy = new LinkedHashMap<>();
	private final Map<PartialRelation, DirectedCrossReferenceInfo> directedCrossReferences = new LinkedHashMap<>();
	private final Map<PartialRelation, UndirectedCrossReferenceInfo> undirectedCrossReferences = new LinkedHashMap<>();
	private final Map<PartialRelation, PartialRelation> oppositeReferences = new LinkedHashMap<>();
	private final Map<AnyPartialFunction, AttributeInfo> attributes = new LinkedHashMap<>();

	MetamodelBuilder() {
		typeHierarchyBuilder.type(ContainmentHierarchyTranslator.CONTAINER_SYMBOL, true);
		typeHierarchyBuilder.type(ContainmentHierarchyTranslator.CONTAINED_SYMBOL, true);
	}

	public MetamodelBuilder type(PartialRelation partialRelation, TypeInfo typeInfo) {
		typeHierarchyBuilder.type(partialRelation, typeInfo);
		return this;

	}

	public MetamodelBuilder type(PartialRelation partialRelation, boolean abstractType,
								 PartialRelation... supertypes) {
		typeHierarchyBuilder.type(partialRelation, abstractType, supertypes);
		return this;
	}

	public MetamodelBuilder type(PartialRelation partialRelation, boolean abstractType,
								 Collection<PartialRelation> supertypes) {
		typeHierarchyBuilder.type(partialRelation, abstractType, supertypes);
		return this;
	}

	public MetamodelBuilder type(PartialRelation partialRelation, boolean abstractType, boolean decide,
								 Collection<PartialRelation> supertypes) {
		typeHierarchyBuilder.type(partialRelation, abstractType, decide, supertypes);
		return this;
	}

	public MetamodelBuilder type(PartialRelation partialRelation, PartialRelation... supertypes) {
		typeHierarchyBuilder.type(partialRelation, supertypes);
		return this;
	}

	public MetamodelBuilder type(PartialRelation partialRelation, Collection<PartialRelation> supertypes) {
		typeHierarchyBuilder.type(partialRelation, supertypes);
		return this;
	}

	public MetamodelBuilder types(Collection<Map.Entry<PartialRelation, TypeInfo>> entries) {
		typeHierarchyBuilder.types(entries);
		return this;
	}

	public MetamodelBuilder types(Map<PartialRelation, TypeInfo> map) {
		typeHierarchyBuilder.types(map);
		return this;
	}

	public MetamodelBuilder reference(PartialRelation linkType, Consumer<ReferenceInfoBuilder> callback) {
		var builder = ReferenceInfo.builder();
		callback.accept(builder);
		return reference(linkType, builder.build());
	}

	public MetamodelBuilder reference(PartialRelation linkType, ReferenceInfo info) {
		if (linkType.arity() != 2) {
			throw new TranslationException(linkType,
					"Only references of arity 2 are supported, got %s with %d instead".formatted(
							linkType, linkType.arity()));
		}
		var putResult = referenceInfoMap.put(linkType, info);
		if (putResult != null && !putResult.equals(info)) {
			throw new TranslationException(linkType, "Duplicate reference info for partial relation: " + linkType);
		}
		return this;
	}

	public MetamodelBuilder references(Collection<Map.Entry<PartialRelation, ReferenceInfo>> entries) {
		for (var entry : entries) {
			reference(entry.getKey(), entry.getValue());
		}
		return this;
	}

	public MetamodelBuilder references(Map<PartialRelation, ReferenceInfo> map) {
		return references(map.entrySet());
	}

	public MetamodelBuilder attribute(AnyPartialFunction attributeType, AttributeInfo info) {
		if (attributeType.arity() != 1) {
			throw new TranslationException(attributeType,
					"Only attributes of arity 1 are supported, got %s with %d instead".formatted(
							attributeType, attributeType.arity()));
		}
		var putResult = attributes.put(attributeType, info);
		if (putResult != null && !putResult.equals(info)) {
			throw new TranslationException(attributeType,
					"Duplicate attribute info for partial function: " + attributeType);
		}
		return this;
	}

	public MetamodelBuilder attributes(Collection<Map.Entry<AnyPartialFunction, AttributeInfo>> entries) {
		for (var entry : entries) {
			attribute(entry.getKey(), entry.getValue());
		}
		return this;
	}

	public MetamodelBuilder attributes(Map<AnyPartialFunction, AttributeInfo> map) {
		return attributes(map.entrySet());
	}

	public Metamodel build() {
		for (var entry : referenceInfoMap.entrySet()) {
			var linkType = entry.getKey();
			var info = entry.getValue();
			processReferenceInfo(linkType, info);
		}
		for (var entry : attributes.entrySet()) {
			var attributeType = entry.getKey();
			var info = entry.getValue();
			processAttributeInfo(attributeType, info);
		}
		typeHierarchyBuilder.setContainerTypes(containerTypes);
		typeHierarchyBuilder.setContainedTypes(containedTypes);
		var typeHierarchy = typeHierarchyBuilder.build();
		return new Metamodel(typeHierarchy, Collections.unmodifiableMap(containmentHierarchy),
				Collections.unmodifiableMap(directedCrossReferences),
				Collections.unmodifiableMap(undirectedCrossReferences),
				Collections.unmodifiableMap(oppositeReferences),
				Collections.unmodifiableMap(attributes));
	}

	private void processReferenceInfo(PartialRelation linkType, ReferenceInfo info) {
		if (oppositeReferences.containsKey(linkType) || containmentHierarchy.containsKey(linkType)) {
			// We already processed this reference while processing its opposite.
			return;
		}
		var sourceType = info.sourceType();
		if (typeHierarchyBuilder.isInvalidType(sourceType)) {
			throw new TranslationException(linkType, "Source type %s of %s is not in type hierarchy"
					.formatted(sourceType, linkType));
		}
		var targetType = info.targetType();
		var opposite = info.opposite();
		Multiplicity targetMultiplicity = UnconstrainedMultiplicity.INSTANCE;
		var defaultValue = info.defaultValue();
		Set<PartialRelation> oppositeSupersets = Set.of();
		if (opposite != null) {
			var oppositeInfo = referenceInfoMap.get(opposite);
			validateOpposite(linkType, info, opposite, oppositeInfo);
			targetMultiplicity = oppositeInfo.multiplicity();
			defaultValue = defaultValue.meet(oppositeInfo.defaultValue());
			if (oppositeInfo.containment()) {
				// Skip processing this reference and process it once we encounter its containment opposite.
				return;
			}
			if (opposite.equals(linkType)) {
				if (!sourceType.equals(targetType)) {
					throw new TranslationException(linkType,
							"Target %s of undirected reference %s differs from source %s".formatted(
									targetType, linkType, sourceType));
				}
				undirectedCrossReferences.put(linkType, new UndirectedCrossReferenceInfo(sourceType,
						info.multiplicity(), defaultValue, info.concretizationSettings(), info.supersets()));
				return;
			}
			oppositeReferences.put(opposite, linkType);
			oppositeSupersets = oppositeInfo.supersets();
		}
		if (info.containment()) {
			processContainmentInfo(linkType, info, targetMultiplicity);
			return;
		}
		directedCrossReferences.put(linkType, new DirectedCrossReferenceInfo(sourceType, info.multiplicity(),
				targetType, targetMultiplicity, defaultValue, info.concretizationSettings(), info.supersets(),
                oppositeSupersets));
	}

	private void processContainmentInfo(PartialRelation linkType, ReferenceInfo info,
										Multiplicity targetMultiplicity) {
		var sourceType = info.sourceType();
		var targetType = info.targetType();
		var opposite = info.opposite();
		if (typeHierarchyBuilder.isInvalidType(targetType)) {
			throw new TranslationException(linkType, "Target type %s of %s is not in type hierarchy"
					.formatted(targetType, linkType));
		}
		if (!UnconstrainedMultiplicity.INSTANCE.equals(targetMultiplicity)) {
			throw new TranslationException(opposite, "Invalid opposite %s with multiplicity %s of containment %s"
					.formatted(opposite, targetMultiplicity, linkType));
		}
		containerTypes.add(sourceType);
		// Avoid creating a cyclic inheritance hierarchy.
		if (!ContainmentHierarchyTranslator.CONTAINED_SYMBOL.equals(targetType)) {
			containedTypes.add(targetType);
		}
		containmentHierarchy.put(linkType, new ContainmentInfo(sourceType, info.multiplicity(), targetType,
                info.concretizationSettings().decide(), info.supersets(),
				info.opposite() == null ? new LinkedHashSet<>() : referenceInfoMap.get(opposite).supersets()));
	}

	private static void validateOpposite(PartialRelation linkType, ReferenceInfo info, PartialRelation opposite,
										 ReferenceInfo oppositeInfo) {
		var sourceType = info.sourceType();
		var targetType = info.targetType();
		if (oppositeInfo == null) {
			throw new TranslationException(linkType, "Opposite %s of %s is not defined"
					.formatted(opposite, linkType));
		}
		if (!linkType.equals(oppositeInfo.opposite())) {
			throw new TranslationException(opposite, "Expected %s to have opposite %s, got %s instead"
					.formatted(opposite, linkType, oppositeInfo.opposite()));
		}
		if (!targetType.equals(oppositeInfo.sourceType())) {
			throw new TranslationException(linkType, "Expected %s to have source type %s, got %s instead"
					.formatted(opposite, targetType, oppositeInfo.sourceType()));
		}
		if (!sourceType.equals(oppositeInfo.targetType())) {
			throw new TranslationException(linkType, "Expected %s to have target type %s, got %s instead"
					.formatted(opposite, sourceType, oppositeInfo.targetType()));
		}
		if (oppositeInfo.containment() && info.containment()) {
			throw new TranslationException(opposite, "Opposite %s of containment %s cannot be containment"
					.formatted(opposite, linkType));
		}
		if (!info.concretizationSettings().equals(oppositeInfo.concretizationSettings())) {
			throw new TranslationException(opposite, "Concretization settings of opposites %s and %s don't match"
					.formatted(opposite, linkType));
		}
	}

	private void processAttributeInfo(AnyPartialFunction attributeType, AttributeInfo info) {
		var owningType = info.owningType();
		if (typeHierarchyBuilder.isInvalidType(owningType)) {
			throw new TranslationException(attributeType, "Owning type %s of %s is not in type hierarchy"
					.formatted(owningType, attributeType));
		}
	}
}
