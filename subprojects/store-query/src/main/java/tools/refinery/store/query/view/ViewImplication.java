/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.query.view;

import java.util.List;

public record ViewImplication(AnySymbolView implyingView, AnySymbolView impliedView, List<Integer> impliedIndices) {
	public ViewImplication {
		if (impliedIndices.size() != impliedView.arity()) {
			throw new IllegalArgumentException("Expected %d implied indices for %s, but %d are provided"
					.formatted(impliedView.arity(), impliedView, impliedIndices.size()));
		}
		for (var index : impliedIndices) {
			if (impliedView.invalidIndex(index)) {
				throw new IllegalArgumentException("%d is not a valid index for %s".formatted(index,
						implyingView));
			}
		}
	}
}
