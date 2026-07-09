/*
 * SPDX-FileCopyrightText: 2026 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package tools.refinery.generator.dto;

import java.util.List;

public class ObjectNetDto {
    private final List<TypeDto> types;
    private final List<RelationTypeDto> relationTypes;
    private final List<InstanceDto> instances;
    private final List<RelationInstanceDto> relationInstances;

    public ObjectNetDto(List<TypeDto> types, List<RelationTypeDto> relationTypes, List<InstanceDto> instances, List<RelationInstanceDto> relationInstances) {
        this.types = types;
        this.relationTypes = relationTypes;
        this.instances = instances;
        this.relationInstances = relationInstances;
    }

    public List<TypeDto> getTypes() {
        return types;
    }

    public List<RelationTypeDto> getRelationTypes() {
        return relationTypes;
    }

    public List<InstanceDto> getInstances() {
        return instances;
    }

    public List<RelationInstanceDto> getRelationInstances() {
        return relationInstances;
    }
}
