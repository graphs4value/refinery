/*
 * SPDX-FileCopyrightText: 2026 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package tools.refinery.generator.dto;

import java.util.ArrayList;
import java.util.List;

public class InstanceDto {
    private final Integer id;
    private final String name;
    private final String principalType;
    private final transient TypeDto principalTypeObject;
    private final List<String> types = new ArrayList<>();
    private final transient List<TypeDto> typeObjects = new ArrayList<>();
    private final transient List<RelationInstanceDto> outgoingRelations = new ArrayList<>();
    private final transient List<RelationInstanceDto> incomingRelations = new ArrayList<>();

    public InstanceDto(Integer id, TypeDto principalType, String name) {
        this.id = id;
        this.name = name;
        this.principalTypeObject = principalType;
        this.principalType = principalType.getName();
    }

    public String getName() {
        return name;
    }

    public Integer getId() {
        return id;
    }

    public TypeDto getPrincipalTypeObject() {
        return principalTypeObject;
    }

    public List<String> getTypes() {
        return types;
    }

    public void addType(TypeDto type) {
        typeObjects.add(type);
        types.add(type.getName());
    }

    public void addTypes(List<TypeDto> types) {
        for (TypeDto type : types) {
            addType(type);
        }
    }

    public List<RelationInstanceDto> getIncomingRelations() {
        return incomingRelations;
    }

    public List<RelationInstanceDto> getOutgoingRelations() {
        return outgoingRelations;
    }
}
