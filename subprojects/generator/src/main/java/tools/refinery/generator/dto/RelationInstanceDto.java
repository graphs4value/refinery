/*
 * SPDX-FileCopyrightText: 2026 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package tools.refinery.generator.dto;

import tools.refinery.store.reasoning.representation.PartialRelation;

public class RelationInstanceDto {

    private final transient PartialRelation relation;
    private final transient RelationTypeDto type;
    private final transient InstanceDto source;
    private final transient InstanceDto target;

    private final String relationType;
    private final Integer sourceId;
    private final Integer targetId;

    public RelationInstanceDto(PartialRelation relation, RelationTypeDto type, InstanceDto source, InstanceDto target) {
        this.relation = relation;
        this.type = type;
        this.source = source;
        this.target = target;
        this.relationType = type.getName();
        this.sourceId = source.getId();
        this.targetId = target.getId();

        source.getOutgoingRelations().add(this);
        target.getIncomingRelations().add(this);
    }

    public RelationTypeDto getType() {
        return type;
    }

    public InstanceDto getSource() {
        return source;
    }

    public InstanceDto getTarget() {
        return target;
    }
}
