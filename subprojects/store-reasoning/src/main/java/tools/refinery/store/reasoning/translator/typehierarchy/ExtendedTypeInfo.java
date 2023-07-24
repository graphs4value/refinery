/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning.translator.typehierarchy;

import org.jetbrains.annotations.NotNull;
import tools.refinery.store.reasoning.representation.PartialRelation;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

final class ExtendedTypeInfo implements Comparable<ExtendedTypeInfo> {
	private final int index;
	private final PartialRelation type;
	private final TypeInfo typeInfo;
	private final Set<PartialRelation> allSubtypes = new LinkedHashSet<>();
	private final Set<PartialRelation> allSupertypes;
	private final Set<PartialRelation> concreteSubtypesAndSelf = new LinkedHashSet<>();
	private Set<PartialRelation> directSubtypes;
	private final Set<PartialRelation> unsortedDirectSupertypes = new HashSet<>();

	public ExtendedTypeInfo(int index, PartialRelation type, TypeInfo typeInfo) {
		this.index = index;
		this.type = type;
		this.typeInfo = typeInfo;
		this.allSupertypes = new LinkedHashSet<>(typeInfo.supertypes());
	}

	public PartialRelation getType() {
		return type;
	}

	public TypeInfo getTypeInfo() {
		return typeInfo;
	}

	public boolean isAbstractType() {
		return getTypeInfo().abstractType();
	}

	public Set<PartialRelation> getAllSubtypes() {
		return allSubtypes;
	}

	public Set<PartialRelation> getAllSupertypes() {
		return allSupertypes;
	}

	public Set<PartialRelation> getAllSupertypesAndSelf() {
		var allSubtypesAndSelf = new HashSet<PartialRelation>(allSupertypes.size() + 1);
		addMust(allSubtypesAndSelf);
		return allSubtypesAndSelf;
	}

	public Set<PartialRelation> getConcreteSubtypesAndSelf() {
		return concreteSubtypesAndSelf;
	}

	public Set<PartialRelation> getDirectSubtypes() {
		return directSubtypes;
	}

	public Set<PartialRelation> getUnsortedDirectSupertypes() {
		return unsortedDirectSupertypes;
	}

	public void setDirectSubtypes(Set<PartialRelation> directSubtypes) {
		this.directSubtypes = directSubtypes;
	}

	public boolean allowsAllConcreteTypes(Set<PartialRelation> concreteTypes) {
		for (var concreteType : concreteTypes) {
			if (!concreteSubtypesAndSelf.contains(concreteType)) {
				return false;
			}
		}
		return true;
	}

	public void addMust(Set<PartialRelation> mustTypes) {
		mustTypes.add(type);
		mustTypes.addAll(allSupertypes);
	}

	@Override
	public int compareTo(@NotNull ExtendedTypeInfo extendedTypeInfo) {
		return Integer.compare(index, extendedTypeInfo.index);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		ExtendedTypeInfo that = (ExtendedTypeInfo) o;
		return index == that.index;
	}

	@Override
	public int hashCode() {
		return Objects.hash(index);
	}
}
