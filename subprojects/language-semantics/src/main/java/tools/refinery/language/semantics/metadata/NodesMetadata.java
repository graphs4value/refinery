/*
 * SPDX-FileCopyrightText: 2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.semantics.metadata;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public record NodesMetadata(List<NodeMetadata> list) {
	@NotNull
	public String getSimpleName(int nodeId) {
		String name = null;
		if (nodeId >= 0 && nodeId < list.size()) {
			var metadata = list.get(nodeId);
			if (metadata != null) {
				name = metadata.simpleName();
			}
		}
		if (name == null) {
			return "::" + nodeId;
		}
		return name;
	}
}
