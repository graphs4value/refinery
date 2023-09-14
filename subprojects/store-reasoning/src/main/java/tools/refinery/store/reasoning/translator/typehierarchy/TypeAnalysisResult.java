/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning.translator.typehierarchy;

import tools.refinery.store.reasoning.representation.PartialRelation;
import tools.refinery.store.representation.TruthValue;

import java.util.*;

public final class TypeAnalysisResult {
	private final ExtendedTypeInfo extendedTypeInfo;
	private final List<PartialRelation> directSubtypes;
	private final List<ExtendedTypeInfo> allExternalTypeInfoList;
	private final InferredType inferredType;

	public TypeAnalysisResult(ExtendedTypeInfo extendedTypeInfo, List<ExtendedTypeInfo> allExternalTypeInfoList) {
		this.extendedTypeInfo = extendedTypeInfo;
		directSubtypes = List.copyOf(extendedTypeInfo.getDirectSubtypes());
		this.allExternalTypeInfoList = allExternalTypeInfoList;
		inferredType = propagateMust(extendedTypeInfo.getAllSupertypesAndSelf(),
				extendedTypeInfo.getConcreteSubtypesAndSelf());
	}

	public PartialRelation type() {
		return extendedTypeInfo.getType();
	}

	public List<PartialRelation> getDirectSubtypes() {
		return directSubtypes;
	}

	public boolean isAbstractType() {
		return extendedTypeInfo.isAbstractType();
	}

	public boolean isVacuous() {
		return isAbstractType() && directSubtypes.isEmpty();
	}

	public InferredType asInferredType() {
		return inferredType;
	}

	public boolean isSubtypeOf(TypeAnalysisResult other) {
		return extendedTypeInfo.getAllSubtypes().contains(other.type());
	}

	public InferredType merge(InferredType inferredType, TruthValue value) {
		return switch (value) {
			case UNKNOWN -> inferredType;
			case TRUE -> addMust(inferredType);
			case FALSE -> removeMay(inferredType);
			case ERROR -> addError(inferredType);
		};
	}

	private InferredType addMust(InferredType inferredType) {
		var originalMustTypes = inferredType.mustTypes();
		if (originalMustTypes.contains(type())) {
			return inferredType;
		}
		var mustTypes = new HashSet<>(originalMustTypes);
		extendedTypeInfo.addMust(mustTypes);
		var originalMayConcreteTypes = inferredType.mayConcreteTypes();
		var mayConcreteTypes = new LinkedHashSet<>(originalMayConcreteTypes);
		Set<PartialRelation> mayConcreteTypesResult;
		if (mayConcreteTypes.retainAll(extendedTypeInfo.getConcreteSubtypesAndSelf())) {
			mayConcreteTypesResult = mayConcreteTypes;
		} else {
			mayConcreteTypesResult = originalMayConcreteTypes;
		}
		return propagateMust(mustTypes, mayConcreteTypesResult);
	}

	private InferredType removeMay(InferredType inferredType) {
		var originalMayConcreteTypes = inferredType.mayConcreteTypes();
		var mayConcreteTypes = new LinkedHashSet<>(originalMayConcreteTypes);
		if (!mayConcreteTypes.removeAll(extendedTypeInfo.getConcreteSubtypesAndSelf())) {
			return inferredType;
		}
		return propagateMust(inferredType.mustTypes(), mayConcreteTypes);
	}

	private InferredType addError(InferredType inferredType) {
		var originalMustTypes = inferredType.mustTypes();
		if (originalMustTypes.contains(type())) {
			if (inferredType.mayConcreteTypes().isEmpty()) {
				return inferredType;
			}
			return new InferredType(originalMustTypes, Set.of(), null);
		}
		var mustTypes = new HashSet<>(originalMustTypes);
		extendedTypeInfo.addMust(mustTypes);
		return new InferredType(mustTypes, Set.of(), null);
	}

	private InferredType propagateMust(Set<PartialRelation> originalMustTypes,
									   Set<PartialRelation> mayConcreteTypes) {
		// It is possible that there is not type at all, do not force one by propagation.
		var maybeUntyped = originalMustTypes.isEmpty();
		// Para-consistent case, do not propagate must types to avoid logical explosion.
		var paraConsistentOrSurelyUntyped = mayConcreteTypes.isEmpty();
		if (maybeUntyped || paraConsistentOrSurelyUntyped) {
			return new InferredType(originalMustTypes, mayConcreteTypes, null);
		}
		var currentType = computeCurrentType(mayConcreteTypes);
		var mustTypes = new HashSet<>(originalMustTypes);
		boolean changed = false;
		for (var newMustExtendedTypeInfo : allExternalTypeInfoList) {
			var newMustType = newMustExtendedTypeInfo.getType();
			if (mustTypes.contains(newMustType)) {
				continue;
			}
			if (newMustExtendedTypeInfo.allowsAllConcreteTypes(mayConcreteTypes)) {
				newMustExtendedTypeInfo.addMust(mustTypes);
				changed = true;
			}
		}
		if (!changed) {
			return new InferredType(originalMustTypes, mayConcreteTypes, currentType);
		}
		return new InferredType(mustTypes, mayConcreteTypes, currentType);
	}

	/**
	 * Returns a concrete type that is allowed by a (consistent, i.e., nonempty) set of <b>may</b> concrete types.
	 *
	 * @param mayConcreteTypes The set of allowed concrete types. Must not be empty.
	 * @return The first concrete type that is allowed by {@code matConcreteTypes}.
	 */
	private PartialRelation computeCurrentType(Set<PartialRelation> mayConcreteTypes) {
		for (var concreteExtendedTypeInfo : allExternalTypeInfoList) {
			var concreteType = concreteExtendedTypeInfo.getType();
			if (!concreteExtendedTypeInfo.isAbstractType() && mayConcreteTypes.contains(concreteType)) {
				return concreteType;
			}
		}
		// We have already filtered out the para-consistent case in {@link #propagateMust(Set<PartialRelation>,
		// Set<PartialRelation>}.
		throw new AssertionError("No concrete type in %s".formatted(mayConcreteTypes));
	}
}
