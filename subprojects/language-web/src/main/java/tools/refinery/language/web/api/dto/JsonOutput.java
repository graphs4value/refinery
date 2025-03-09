/*
 * SPDX-FileCopyrightText: 2025 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.web.api.dto;

import com.google.gson.JsonObject;
import tools.refinery.language.semantics.metadata.NodeMetadata;
import tools.refinery.language.semantics.metadata.RelationMetadata;

import java.util.List;

public record JsonOutput(List<NodeMetadata> nodes, List<RelationMetadata> relations,
						 JsonObject partialInterpretation) {
}
