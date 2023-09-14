/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.query.view;

import tools.refinery.store.model.Model;
import tools.refinery.store.query.dnf.FunctionalDependency;
import tools.refinery.store.representation.AnySymbol;
import tools.refinery.store.query.Constraint;

import java.util.Set;

public sealed interface AnySymbolView extends Constraint permits SymbolView {
	AnySymbol getSymbol();

	String getViewName();

	default Set<FunctionalDependency<Integer>> getFunctionalDependencies() {
		return Set.of();
	}

	default Set<ViewImplication> getImpliedRelationViews() {
		return Set.of();
	}

	boolean get(Model model, Object[] tuple);

	Iterable<Object[]> getAll(Model model);

	default Iterable<Object[]> getAdjacent(Model model, int slot, Object value) {
		throw new IllegalArgumentException("Cannot index slot " + slot);
	}

	default boolean canIndexSlot(int slot) {
		return false;
	}
}
