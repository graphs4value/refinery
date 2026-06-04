/*
 * SPDX-FileCopyrightText: 2026 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package tools.refinery.generator.dto;

import tools.refinery.store.reasoning.representation.PartialRelation;

import java.util.ArrayList;
import java.util.List;

public class TypeDto {
    private final transient PartialRelation relation;
    private final String name;
    private final List<String> superTypes = new ArrayList<>();
    private final transient List<TypeDto> superTypeObjects = new ArrayList<>();
    private final transient List<TypeDto> subTypeObjects = new ArrayList<>();

    public TypeDto(PartialRelation relation, String name) {
        this.relation = relation;
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public PartialRelation getPartialRelation() {
        return relation;
    }

    public List<String> getSuperTypes() {
        return superTypes;
    }

    public List<TypeDto> getSuperTypeObjects() {
        return superTypeObjects;
    }

    public void addSuperType(TypeDto type) {
        superTypeObjects.add(type);
        superTypes.add(type.getName());
    }

    public void addSubType(TypeDto type) {
        subTypeObjects.add(type);
    }
}
