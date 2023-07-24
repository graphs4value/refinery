/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.utils;

import java.util.Map;

import tools.refinery.language.model.problem.Node;
import tools.refinery.language.model.problem.Relation;

public record CollectedSymbols(Map<Node, NodeInfo> nodes, Map<Relation, RelationInfo> relations) {

}
