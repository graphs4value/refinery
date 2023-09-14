/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning.refinement;

import tools.refinery.store.model.Model;
import tools.refinery.store.representation.Symbol;

public interface StorageRefiner {
	boolean split(int parentNode, int childNode);

	boolean cleanup(int nodeToDelete);

	@FunctionalInterface
	interface Factory<T> {
		StorageRefiner create(Symbol<T> symbol, Model model);
	}
}
