/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning.translator.typehierarchy;

import tools.refinery.store.reasoning.representation.PartialRelation;
import tools.refinery.store.reasoning.translator.TranslationException;

import java.util.*;

public class TypeHierarchy {
	private final Set<PartialRelation> allTypes;
	private final Map<PartialRelation, ExtendedTypeInfo> extendedTypeInfoMap;
	private final Map<PartialRelation, PartialRelation> replacements = new LinkedHashMap<>();
	private final InferredType unknownType;
	private final Map<PartialRelation, TypeAnalysisResult> preservedTypes;

	TypeHierarchy(Map<PartialRelation, TypeInfo> typeInfoMap) {
		int size = typeInfoMap.size();
		allTypes = Collections.unmodifiableSet(new LinkedHashSet<>(typeInfoMap.keySet()));
		extendedTypeInfoMap = new LinkedHashMap<>(size);
		var concreteTypes = new LinkedHashSet<PartialRelation>();
		int index = 0;
		for (var entry : typeInfoMap.entrySet()) {
			var type = entry.getKey();
			var typeInfo = entry.getValue();
			extendedTypeInfoMap.put(type, new ExtendedTypeInfo(index, type, typeInfo));
			if (!typeInfo.abstractType()) {
				concreteTypes.add(type);
			}
			index++;
		}
		unknownType = new InferredType(Set.of(), concreteTypes, null);
		computeAllSupertypes();
		computeAllAndConcreteSubtypes();
		computeDirectSubtypes();
		eliminateTrivialSupertypes();
		preservedTypes = computeAnalysisResults();
	}

	public boolean isEmpty() {
		return extendedTypeInfoMap.isEmpty();
	}

	public InferredType getUnknownType() {
		return unknownType;
	}

	public Set<PartialRelation> getAllTypes() {
		return allTypes;
	}

	public Map<PartialRelation, TypeAnalysisResult> getPreservedTypes() {
		return preservedTypes;
	}

	public Map<PartialRelation, PartialRelation> getEliminatedTypes() {
		return Collections.unmodifiableMap(replacements);
	}

	public TypeAnalysisResult getAnalysisResult(PartialRelation type) {
		var preservedResult = preservedTypes.get(type);
		if (preservedResult != null) {
			return preservedResult;
		}
		var eliminatedResult = replacements.get(type);
		if (eliminatedResult != null) {
			return preservedTypes.get(eliminatedResult);
		}
		throw new IllegalArgumentException("Unknown type: " + type);
	}

	private void computeAllSupertypes() {
		boolean changed;
		do {
			changed = false;
			for (var extendedTypeInfo : extendedTypeInfoMap.values()) {
				var found = new HashSet<PartialRelation>();
				var allSupertypes = extendedTypeInfo.getAllSupertypes();
				for (var supertype : allSupertypes) {
					var supertypeInfo = extendedTypeInfoMap.get(supertype);
					if (supertypeInfo == null) {
						throw new TranslationException(extendedTypeInfo.getType(),
								"Supertype %s of %s is missing from the type hierarchy"
										.formatted(supertype, extendedTypeInfo.getType()));
					}
					found.addAll(supertypeInfo.getAllSupertypes());
				}
				if (allSupertypes.addAll(found)) {
					changed = true;
				}
			}
		} while (changed);
	}

	private void computeAllAndConcreteSubtypes() {
		for (var extendedTypeInfo : extendedTypeInfoMap.values()) {
			var type = extendedTypeInfo.getType();
			if (!extendedTypeInfo.isAbstractType()) {
				extendedTypeInfo.getConcreteSubtypesAndSelf().add(type);
			}
			for (var supertype : extendedTypeInfo.getAllSupertypes()) {
				if (type.equals(supertype)) {
					throw new TranslationException(type, "%s cannot be a supertype of itself".formatted(type));
				}
				var supertypeInfo = extendedTypeInfoMap.get(supertype);
				supertypeInfo.getAllSubtypes().add(type);
				if (!extendedTypeInfo.isAbstractType()) {
					supertypeInfo.getConcreteSubtypesAndSelf().add(type);
				}
			}
		}
	}

	private void computeDirectSubtypes() {
		for (var extendedTypeInfo : extendedTypeInfoMap.values()) {
			var allSubtypes = extendedTypeInfo.getAllSubtypes();
			var directSubtypes = new LinkedHashSet<>(allSubtypes);
			var indirectSubtypes = new LinkedHashSet<PartialRelation>(allSubtypes.size());
			for (var subtype : allSubtypes) {
				indirectSubtypes.addAll(extendedTypeInfoMap.get(subtype).getAllSubtypes());
			}
			directSubtypes.removeAll(indirectSubtypes);
			extendedTypeInfo.setDirectSubtypes(directSubtypes);
		}
	}

	private void eliminateTrivialSupertypes() {
		Set<PartialRelation> toInspect = new HashSet<>(extendedTypeInfoMap.keySet());
		while (!toInspect.isEmpty()) {
			var toRemove = new ArrayList<PartialRelation>();
			for (var partialRelation : toInspect) {
				var extendedTypeInfo = extendedTypeInfoMap.get(partialRelation);
				if (extendedTypeInfo != null && isTrivialSupertype(extendedTypeInfo)) {
					toRemove.add(partialRelation);
				}
			}
			toInspect.clear();
			for (var partialRelation : toRemove) {
				removeTrivialType(partialRelation, toInspect);
			}
		}
	}

	private boolean isTrivialSupertype(ExtendedTypeInfo extendedTypeInfo) {
		if (!extendedTypeInfo.isAbstractType()) {
			return false;
		}
		var subtypeIterator = extendedTypeInfo.getDirectSubtypes().iterator();
		if (!subtypeIterator.hasNext()) {
			// Do not eliminate abstract types with 0 subtypes, because they can be used para-consistently, i.e.,
			// an object determined to <b>must</b> have an abstract type with 0 subtypes <b>may not</b> ever exist.
			return false;
		}
		var directSubtype = subtypeIterator.next();
		return !extendedTypeInfoMap.get(directSubtype).isAbstractType() && !subtypeIterator.hasNext();
	}

	private void removeTrivialType(PartialRelation trivialType, Set<PartialRelation> toInspect) {
		var extendedTypeInfo = extendedTypeInfoMap.get(trivialType);
		var iterator = extendedTypeInfo.getDirectSubtypes().iterator();
		if (!iterator.hasNext()) {
			throw new AssertionError("Expected trivial supertype %s to have a direct subtype"
					.formatted(trivialType));
		}
		PartialRelation replacement = setReplacement(trivialType, iterator.next());
		if (iterator.hasNext()) {
			throw new AssertionError("Expected trivial supertype %s to have at most 1 direct subtype"
					.formatted(trivialType));
		}
		for (var supertype : extendedTypeInfo.getAllSupertypes()) {
			var extendedSupertypeInfo = extendedTypeInfoMap.get(supertype);
			if (!extendedSupertypeInfo.getAllSubtypes().remove(trivialType)) {
				throw new AssertionError("Expected %s to be subtype of %s".formatted(trivialType, supertype));
			}
			var directSubtypes = extendedSupertypeInfo.getDirectSubtypes();
			if (directSubtypes.remove(trivialType)) {
				directSubtypes.add(replacement);
				if (extendedSupertypeInfo.isAbstractType() && directSubtypes.size() == 1) {
					toInspect.add(supertype);
				}
			}
		}
		for (var subtype : extendedTypeInfo.getAllSubtypes()) {
			var extendedSubtypeInfo = extendedTypeInfoMap.get(subtype);
			if (!extendedSubtypeInfo.getAllSupertypes().remove(trivialType)) {
				throw new AssertionError("Expected %s to be supertype of %s".formatted(trivialType, subtype));
			}
		}
		extendedTypeInfoMap.remove(trivialType);
	}

	private PartialRelation setReplacement(PartialRelation trivialRelation, PartialRelation replacement) {
		if (replacement == null) {
			return trivialRelation;
		}
		var resolved = setReplacement(replacement, replacements.get(replacement));
		replacements.put(trivialRelation, resolved);
		return resolved;
	}

	private Map<PartialRelation, TypeAnalysisResult> computeAnalysisResults() {
		var allExtendedTypeInfoList = sortTypes();
		var preservedResults = new LinkedHashMap<PartialRelation, TypeAnalysisResult>(
				allExtendedTypeInfoList.size());
		for (var extendedTypeInfo : allExtendedTypeInfoList) {
			var type = extendedTypeInfo.getType();
			preservedResults.put(type, new TypeAnalysisResult(extendedTypeInfo, allExtendedTypeInfoList));
		}
		return Collections.unmodifiableMap(preservedResults);
	}

	private List<ExtendedTypeInfo> sortTypes() {
		// Invert {@code directSubtypes} to keep track of the out-degree of types.
		for (var extendedTypeInfo : extendedTypeInfoMap.values()) {
			for (var directSubtype : extendedTypeInfo.getDirectSubtypes()) {
				var extendedDirectSubtypeInfo = extendedTypeInfoMap.get(directSubtype);
				extendedDirectSubtypeInfo.getUnsortedDirectSupertypes().add(extendedTypeInfo.getType());
			}
		}
		// Build a <i>inverse</i> topological order ({@code extends} edges always points to earlier nodes in the order,
		// breaking ties according to the original order ({@link ExtendedTypeInfo#index}) to form a 'stable' sort.
		// See, e.g., https://stackoverflow.com/a/11236027.
		var priorityQueue = new PriorityQueue<ExtendedTypeInfo>();
		for (var extendedTypeInfo : extendedTypeInfoMap.values()) {
			if (extendedTypeInfo.getUnsortedDirectSupertypes().isEmpty()) {
				priorityQueue.add(extendedTypeInfo);
			}
		}
		var sorted = new ArrayList<ExtendedTypeInfo>(extendedTypeInfoMap.size());
		while (!priorityQueue.isEmpty()) {
			var extendedTypeInfo = priorityQueue.remove();
			sorted.add(extendedTypeInfo);
			for (var directSubtype : extendedTypeInfo.getDirectSubtypes()) {
				var extendedDirectSubtypeInfo = extendedTypeInfoMap.get(directSubtype);
				var unsortedDirectSupertypes = extendedDirectSubtypeInfo.getUnsortedDirectSupertypes();
				if (!unsortedDirectSupertypes.remove(extendedTypeInfo.getType())) {
					throw new AssertionError("Expected %s to be a direct supertype of %s"
							.formatted(extendedTypeInfo.getType(), directSubtype));
				}
				if (unsortedDirectSupertypes.isEmpty()) {
					priorityQueue.add(extendedDirectSubtypeInfo);
				}
			}
		}
		return Collections.unmodifiableList(sorted);
	}

	public static TypeHierarchyBuilder builder() {
		return new TypeHierarchyBuilder();
	}
}
