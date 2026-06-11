/*
 * SPDX-FileCopyrightText: 2021-2026 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.model;

import tools.refinery.store.adapter.ModelAdapter;
import tools.refinery.store.map.Version;
import tools.refinery.store.map.Versioned;
import tools.refinery.store.representation.AnySymbol;
import tools.refinery.store.representation.Symbol;
import tools.refinery.store.util.CancellationToken;

import java.util.Optional;

public interface Model extends Versioned, AutoCloseable {
	Version NO_STATE_ID = null;

	ModelStore getStore();

	Version getState();

	boolean hasUncommittedChanges();

	default AnyInterpretation getInterpretation(AnySymbol symbol) {
		return getInterpretation((Symbol<?>) symbol);
	}

	<T> Interpretation<T> getInterpretation(Symbol<T> symbol);

	default ModelDiffCursor getDiffCursor(Version to) {
		return getDiffCursor(to, false);
	}

	ModelDiffCursor getDiffCursor(Version to, boolean consolidate);

	<T extends ModelAdapter> Optional<T> tryGetAdapter(Class<? extends T> adapterType);

	<T extends ModelAdapter> T getAdapter(Class<T> adapterType);

	void addListener(ModelListener listener);

	void removeListener(ModelListener listener);

	CancellationToken getCancellationToken();

	void checkCancelled();

	@Override
	void close();
}
