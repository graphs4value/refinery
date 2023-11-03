/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.web.semantics;

import com.google.gson.JsonObject;
import tools.refinery.language.web.semantics.metadata.NodeMetadata;
import tools.refinery.language.web.semantics.metadata.RelationMetadata;

import java.util.List;

public record SemanticsSuccessResult(List<NodeMetadata> nodes, List<RelationMetadata> relations,
									 JsonObject partialInterpretation) implements SemanticsResult {
}
