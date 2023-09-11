/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.model;

import tools.refinery.store.adapter.ModelAdapter;
import tools.refinery.store.map.Version;
import tools.refinery.store.map.Versioned;
import tools.refinery.store.representation.AnySymbol;
import tools.refinery.store.representation.Symbol;

import java.util.Optional;

public interface Model extends Versioned {
	Version NO_STATE_ID = null;
	ModelStore getStore();

	Version getState();

	boolean hasUncommittedChanges();

	default AnyInterpretation getInterpretation(AnySymbol symbol) {
		return getInterpretation((Symbol<?>) symbol);
	}

	<T> Interpretation<T> getInterpretation(Symbol<T> symbol);

	ModelDiffCursor getDiffCursor(Version to);

	<T extends ModelAdapter> Optional<T> tryGetAdapter(Class<? extends T> adapterType);

	<T extends ModelAdapter> T getAdapter(Class<T> adapterType);

	void addListener(ModelListener listener);

	void removeListener(ModelListener listener);

	void checkCancelled();
}
