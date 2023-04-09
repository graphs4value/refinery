/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.query.view;

import java.util.List;

public record RelationViewImplication(AnyRelationView implyingRelationView, AnyRelationView impliedRelationView,
									  List<Integer> impliedIndices) {
	public RelationViewImplication {
		if (impliedIndices.size() != impliedRelationView.arity()) {
			throw new IllegalArgumentException("Expected %d implied indices for %s, but %d are provided"
					.formatted(impliedRelationView.arity(), impliedRelationView, impliedIndices.size()));
		}
		for (var index : impliedIndices) {
			if (impliedRelationView.invalidIndex(index)) {
				throw new IllegalArgumentException("%d is not a valid index for %s".formatted(index,
						implyingRelationView));
			}
		}
	}
}
