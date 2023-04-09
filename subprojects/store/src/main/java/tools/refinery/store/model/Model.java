/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.model;

import tools.refinery.store.adapter.ModelAdapter;
import tools.refinery.store.adapter.ModelAdapterType;
import tools.refinery.store.map.Versioned;
import tools.refinery.store.representation.AnySymbol;
import tools.refinery.store.representation.Symbol;

import java.util.Optional;

public interface Model extends Versioned {
	long NO_STATE_ID = -1;

	ModelStore getStore();

	long getState();

	boolean hasUncommittedChanges();

	default AnyInterpretation getInterpretation(AnySymbol symbol) {
		return getInterpretation((Symbol<?>) symbol);
	}

	<T> Interpretation<T> getInterpretation(Symbol<T> symbol);

	ModelDiffCursor getDiffCursor(long to);

	<T extends ModelAdapter> Optional<T> tryGetAdapter(ModelAdapterType<? extends T, ?, ?> adapterType);

	<T extends ModelAdapter> T getAdapter(ModelAdapterType<T, ?, ?> adapterType);

	void addListener(ModelListener listener);

	void removeListener(ModelListener listener);
}
