/*
 * SPDX-FileCopyrightText: 2023-2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning.translator.metamodel;

import tools.refinery.store.reasoning.representation.PartialRelation;
import tools.refinery.store.reasoning.translator.TranslationException;
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

	MetamodelBuilder() {
		typeHierarchyBuilder.type(ContainmentHierarchyTranslator.CONTAINER_SYMBOL, true);
		typeHierarchyBuilder.type(ContainmentHierarchyTranslator.CONTAINED_SYMBOL, true);
	}

	//ezek csinálják a különböző typeokat
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

	//ezek meg a különböző referenceket
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

	//Ez észíti el összességében az egész metamodelt
	public Metamodel build() {
		//Minden referenceInfoMap-ben lévő referenciát feldolgoz és meghívja a processReferenceInfo-ta típusával
		// (Partial Relation) és az info-jaival (ReferenceInfo).
		for (var entry : referenceInfoMap.entrySet()) {
			var linkType = entry.getKey();
			var info = entry.getValue();
			processReferenceInfo(linkType, info);
		}
		//Beállítja a containereket és containmenteket
		typeHierarchyBuilder.setContainerTypes(containerTypes);
		typeHierarchyBuilder.setContainedTypes(containedTypes);
		//Típus hierarchiát épít
		var typeHierarchy = typeHierarchyBuilder.build();
		//Visszaadja a metamodelt a conatinment hierarchibával, directed és undirected cross reference-ekkel és az
		// opposite reference-ekkel.
		return new Metamodel(typeHierarchy, Collections.unmodifiableMap(containmentHierarchy),
				Collections.unmodifiableMap(directedCrossReferences),
				Collections.unmodifiableMap(undirectedCrossReferences),
				Collections.unmodifiableMap(oppositeReferences));
	}

	// A referenciák feldolgozása
	private void processReferenceInfo(PartialRelation linkType, ReferenceInfo info) {
		//ha az oppositeReferences tartalmazza a linkType-ot vagy a containmentHierarchy tartalmazza a linkType-ot
		// akkor ezt már feldolgoztuk.
		if (oppositeReferences.containsKey(linkType) || containmentHierarchy.containsKey(linkType)) {
			// We already processed this reference while processing its opposite.
			return;
		}
		//A ReferenceInfo-ból szedje ki a sourceTypeot.
		var sourceType = info.sourceType();
		//Ha a typeHierarchybuilder szerint invalid a sourceType akkor dobjon egy TranslationException-t.
		if (typeHierarchyBuilder.isInvalidType(sourceType)) {
			throw new TranslationException(linkType, "Source type %s of %s is not in type hierarchy"
					.formatted(sourceType, linkType));
		}
		//A ReferenceInfoból szedje ki a targetTypeot.
		var targetType = info.targetType();
		//A ReferenceInfoból szedje ki az oppositeot.
		var opposite = info.opposite();
		//Singletonra állítja a targetMultiplicityt. (?)
		Multiplicity targetMultiplicity = UnconstrainedMultiplicity.INSTANCE;
		//A ReferenceInfoból szedje ki a reference default truth valueját.
		var defaultValue = info.defaultValue();
		LinkedHashSet<PartialRelation> oppositeSupersets = new LinkedHashSet<>();
		//Ha van a referencenek van oppositeja.
		if (opposite != null) {
			//Keresse ki a referenceInfoMapből az oppositeot.
			var oppositeInfo = referenceInfoMap.get(opposite);
			//Opposite validálása, hogy működhet-e.
			validateOpposite(linkType, info, opposite, oppositeInfo);
			//Az opposite info multiplicitása.
			targetMultiplicity = oppositeInfo.multiplicity();
			//A reference default truth valueját az opposite default truth valuejával egyesítse megadott szabályok
			// mentén.
			defaultValue = defaultValue.meet(oppositeInfo.defaultValue());
			if (oppositeInfo.containment()) {
				// Skip processing this reference and process it once we encounter its containment opposite.
				return;
			}
			//Ha az opposite és a linkType megegyezik.
			if (opposite.equals(linkType)) {
				//Ha a sourceType és a targetType nem egyezik meg akkor dobjon egy TranslationException-t. (UNDIRECTED)
				if (!sourceType.equals(targetType)) {
					throw new TranslationException(linkType,
							"Target %s of undirected reference %s differs from source %s".formatted(
									targetType, linkType, sourceType));
				}
				//Rakja be az undirected cross referencebe
				undirectedCrossReferences.put(linkType, new UndirectedCrossReferenceInfo(sourceType,
						info.multiplicity(), defaultValue, info.partial(), info.supersets()));
				return;
			}
			//ha van oppositja de nem containment és nem egyeznek meg akkor oppositereferencebe rakja be DE NEM LÉP KI.
			oppositeReferences.put(opposite, linkType);
			oppositeSupersets.addAll(oppositeInfo.supersets());
		}
		//Ha a reference containment
		if (info.containment()) {
			processContainmentInfo(linkType, info, targetMultiplicity);
			return;
		}
		//Ha nincs opposite és nem containment akkor directed cross referencebe rakja be.
		directedCrossReferences.put(linkType, new DirectedCrossReferenceInfo(sourceType, info.multiplicity(),
				targetType, targetMultiplicity, defaultValue, info.partial(), info.supersets(), oppositeSupersets));
	}

	//Containmentek feldolgozására
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
		containedTypes.add(targetType);
		containmentHierarchy.put(linkType, new ContainmentInfo(sourceType, info.multiplicity(), targetType, info.supersets(),
				info.opposite() == null ? new LinkedHashSet<>() : referenceInfoMap.get(opposite).supersets()));
	}

	//Kell hozzá a reference és referenceInfoja, az opposite és az oppositeInfoja. (mindkét oldal infóostól)
	private static void validateOpposite(PartialRelation linkType, ReferenceInfo info, PartialRelation opposite,
										 ReferenceInfo oppositeInfo) {
		//sima reference source és target type.
		var sourceType = info.sourceType();
		var targetType = info.targetType();
		//Ha nincs oppositeInfo akkor dobjon egy TranslationException-t.
		if (oppositeInfo == null) {
			throw new TranslationException(linkType, "Opposite %s of %s is not defined"
					.formatted(opposite, linkType));
		}
		//Ha az opposite infójában az opposite nem ez.
		if (!linkType.equals(oppositeInfo.opposite())) {
			throw new TranslationException(opposite, "Expected %s to have opposite %s, got %s instead"
					.formatted(opposite, linkType, oppositeInfo.opposite()));
		}
		//Ha a végek nem egyeznek
		if (!targetType.equals(oppositeInfo.sourceType())) {
			throw new TranslationException(linkType, "Expected %s to have source type %s, got %s instead"
					.formatted(opposite, targetType, oppositeInfo.sourceType()));
		}
		if (!sourceType.equals(oppositeInfo.targetType())) {
			throw new TranslationException(linkType, "Expected %s to have target type %s, got %s instead"
					.formatted(opposite, sourceType, oppositeInfo.targetType()));
		}
		//Opposite és ez is nem lehet containment (körkörös lenne)
		if (oppositeInfo.containment() && info.containment()) {
			throw new TranslationException(opposite, "Opposite %s of containment %s cannot be containment"
					.formatted(opposite, linkType));
		}
		//Ha az egyik partial és a másik nem akkor dobjon egy TranslationException-t.
		if (info.partial() != oppositeInfo.partial()) {
			throw new TranslationException(opposite, "Either both %s and %s have to be partial or neither of them"
					.formatted(opposite, linkType));
		}
	}
}
