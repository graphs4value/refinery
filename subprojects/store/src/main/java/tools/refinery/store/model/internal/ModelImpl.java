/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.model.internal;

import tools.refinery.store.adapter.AdapterUtils;
import tools.refinery.store.adapter.ModelAdapter;
import tools.refinery.store.map.DiffCursor;
import tools.refinery.store.map.Version;
import tools.refinery.store.model.*;
import tools.refinery.store.representation.AnySymbol;
import tools.refinery.store.representation.Symbol;
import tools.refinery.store.tuple.Tuple;
import tools.refinery.store.util.CancellationToken;

import java.util.*;

public class ModelImpl implements Model {
	private final ModelStoreImpl store;
	private Version state;
	private LinkedHashMap<? extends AnySymbol, ? extends VersionedInterpretation<?>> interpretations;
	private final List<ModelAdapter> adapters;
	private final List<ModelListener> listeners = new ArrayList<>();
	private final CancellationToken cancellationToken;
	private boolean uncommittedChanges;
	private ModelAction pendingAction = ModelAction.NONE;
	private Version restoringToState = null;

	ModelImpl(ModelStoreImpl store, Version state, int adapterCount) {
		this.store = store;
		this.state = state;
		adapters = new ArrayList<>(adapterCount);
		cancellationToken = store.getCancellationToken();
	}

	void setInterpretations(LinkedHashMap<? extends AnySymbol, ? extends VersionedInterpretation<?>> interpretations) {
		this.interpretations = interpretations;
	}

	@Override
	public ModelStore getStore() {
		return store;
	}

	@Override
	public Version getState() {
		return state;
	}

	@Override
	public <T> Interpretation<T> getInterpretation(Symbol<T> symbol) {
		var interpretation = interpretations.get(symbol);
		if (interpretation == null) {
			throw new IllegalArgumentException("No interpretation for symbol %s in model".formatted(symbol));
		}
		@SuppressWarnings("unchecked")
		var typedInterpretation = (Interpretation<T>) interpretation;
		return typedInterpretation;
	}

	@Override
	public ModelDiffCursor getDiffCursor(Version to) {
		var diffCursors = new HashMap<AnySymbol, DiffCursor<Tuple, ?>>(interpretations.size());
		for (var entry : interpretations.entrySet()) {
			diffCursors.put(entry.getKey(), entry.getValue().getDiffCursor(to));
		}
		return new ModelDiffCursor(diffCursors);
	}

	private void setState(Version state) {
		this.state = state;
		uncommittedChanges = false;
	}

	void markAsChanged() {
		if (!uncommittedChanges) {
			uncommittedChanges = true;
		}
	}

	@Override
	public boolean hasUncommittedChanges() {
		return uncommittedChanges;
	}

	private boolean hasPendingAction() {
		return pendingAction != ModelAction.NONE || restoringToState != null;
	}

	@Override
	public Version commit() {
		checkCancelled();
		if (hasPendingAction()) {
			throw pendingActionError("commit");
		}
		pendingAction = ModelAction.COMMIT;
		try {
			int listenerCount = listeners.size();
			int i = listenerCount;

			// Before commit message to listeners
			while (i > 0) {
				i--;
				listeners.get(i).beforeCommit();
			}

			// Doing the commit on the interpretations
			Version[] interpretationVersions = new Version[interpretations.size()];
			int j = 0;
			for (var interpretationEntry : interpretations.entrySet()) {
				checkCancelled();
				interpretationVersions[j++] = interpretationEntry.getValue().commit();
			}
			ModelVersion modelVersion = new ModelVersion(interpretationVersions);
			setState(modelVersion);

			// After commit message to listeners
			while (i < listenerCount) {
				listeners.get(i).afterCommit();
				i++;
			}

			return modelVersion;
		} finally {
			pendingAction = ModelAction.NONE;
		}
	}

	@Override
	public void restore(Version version) {
		checkCancelled();
		if (hasPendingAction()) {
			throw pendingActionError("restore to %s".formatted(version));
		}

		pendingAction = ModelAction.RESTORE;
		restoringToState = version;
		try {
			int listenerCount = listeners.size();
			int i = listenerCount;
			while (i > 0) {
				i--;
				listeners.get(i).beforeRestore(version);
			}
			int j = 0;
			for (var interpretation : interpretations.values()) {
				checkCancelled();
				interpretation.restore(ModelVersion.getInternalVersion(version, j++));
			}

			setState(version);
			while (i < listenerCount) {
				listeners.get(i).afterRestore();
				i++;
			}
		} finally {
			pendingAction = ModelAction.NONE;
			restoringToState = null;
		}
	}

	public RuntimeException pendingActionError(String currentActionName) {
		var pendingActionName = switch (pendingAction) {
			case NONE -> throw new IllegalArgumentException("Trying to throw pending action error when there is no " +
					"pending action");
			case COMMIT -> "commit";
			case RESTORE -> "restore to %s".formatted(restoringToState);
		};
		return new IllegalStateException("Cannot %s due to pending %s".formatted(currentActionName, pendingActionName));
	}

	@Override
	public <T extends ModelAdapter> Optional<T> tryGetAdapter(Class<? extends T> adapterType) {
		return AdapterUtils.tryGetAdapter(adapters, adapterType);
	}

	@Override
	public <T extends ModelAdapter> T getAdapter(Class<T> adapterType) {
		return AdapterUtils.getAdapter(adapters, adapterType);
	}

	void addAdapter(ModelAdapter adapter) {
		adapters.add(adapter);
	}

	@Override
	public void addListener(ModelListener listener) {
		listeners.add(listener);
	}

	@Override
	public void removeListener(ModelListener listener) {
		listeners.remove(listener);
	}

	@Override
	public void checkCancelled() {
		cancellationToken.checkCancelled();
	}
}
