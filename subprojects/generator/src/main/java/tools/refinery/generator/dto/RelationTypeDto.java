/*
 * SPDX-FileCopyrightText: 2026 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package tools.refinery.generator.dto;

import tools.refinery.store.reasoning.representation.PartialRelation;

public class RelationTypeDto {

    public enum Kind {
        CONTAINMENT,
        DIRECTED_CROSS_REF,
        UNDIRECTED_CROSS_REF,
        OPPOSITE
    }

    private final transient PartialRelation relation;
    private final Kind kind;
    private final String name;
    private final transient TypeDto source;
    private final transient TypeDto target;
    private final String sourceName;
    private final String targetName;

    public RelationTypeDto(PartialRelation relation, Kind kind, String name, TypeDto source, TypeDto target) {
        this.relation = relation;
        this.kind = kind;
        this.name = name.substring(name.lastIndexOf(":") + 1);
        this.source = source;
        this.target = target;
        this.sourceName = source.getName();
        this.targetName = target.getName();
    }

    public String getName() {
        return name;
    }

    public TypeDto getSource() {
        return source;
    }

    public TypeDto getTarget() {
        return target;
    }

    public PartialRelation getRelation() {
        return relation;
    }

    public Kind getKind() {
        return kind;
    }
}
