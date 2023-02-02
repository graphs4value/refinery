package tools.refinery.store.model.internal;

import tools.refinery.store.adapter.AdapterList;
import tools.refinery.store.adapter.AnyModelAdapterType;
import tools.refinery.store.adapter.ModelAdapter;
import tools.refinery.store.adapter.ModelAdapterType;
import tools.refinery.store.map.DiffCursor;
import tools.refinery.store.model.*;
import tools.refinery.store.representation.AnySymbol;
import tools.refinery.store.representation.Symbol;
import tools.refinery.store.tuple.Tuple;

import java.util.*;

public class ModelImpl implements Model {
	private final ModelStore store;
	private long state;
	private Map<? extends AnySymbol, ? extends VersionedInterpretation<?>> interpretations;
	private final AdapterList<ModelAdapter> adapters;
	private final List<ModelListener> listeners = new ArrayList<>();
	private boolean uncommittedChanges;
	private ModelAction pendingAction = ModelAction.NONE;
	private long restoringToState = NO_STATE_ID;

	ModelImpl(ModelStore store, long state, int adapterCount) {
		this.store = store;
		this.state = state;
		adapters = new AdapterList<>(adapterCount);
	}

	void setInterpretations(Map<? extends AnySymbol, ? extends VersionedInterpretation<?>> interpretations) {
		this.interpretations = interpretations;
	}

	@Override
	public ModelStore getStore() {
		return store;
	}

	@Override
	public long getState() {
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
	public ModelDiffCursor getDiffCursor(long to) {
		var diffCursors = new HashMap<AnySymbol, DiffCursor<Tuple, ?>>(interpretations.size());
		for (var entry : interpretations.entrySet()) {
			diffCursors.put(entry.getKey(), entry.getValue().getDiffCursor(to));
		}
		return new ModelDiffCursor(diffCursors);
	}

	private void setState(long state) {
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
		return pendingAction != ModelAction.NONE || restoringToState != NO_STATE_ID;
	}

	@Override
	public long commit() {
		if (hasPendingAction()) {
			throw pendingActionError("commit");
		}
		pendingAction = ModelAction.COMMIT;
		try {
			int listenerCount = listeners.size();
			int i = listenerCount;
			long version = 0;
			while (i > 0) {
				i--;
				listeners.get(i).beforeCommit();
			}
			boolean versionSet = false;
			for (var interpretation : interpretations.values()) {
				long newVersion = interpretation.commit();
				if (versionSet) {
					if (version != newVersion) {
						throw new IllegalStateException("Interpretations in model have different versions (%d and %d)"
								.formatted(version, newVersion));
					}
				} else {
					version = newVersion;
					versionSet = true;
				}
			}
			setState(version);
			while (i < listenerCount) {
				listeners.get(i).afterCommit();
				i++;
			}
			return version;
		} finally {
			pendingAction = ModelAction.NONE;
		}
	}

	@Override
	public void restore(long version) {
		if (hasPendingAction()) {
			throw pendingActionError("restore to %d".formatted(version));
		}
		if (!store.getStates().contains(version)) {
			throw new IllegalArgumentException("Store does not contain state %d".formatted(version));
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
			for (var interpretation : interpretations.values()) {
				interpretation.restore(version);
			}
			setState(version);
			while (i < listenerCount) {
				listeners.get(i).afterRestore();
				i++;
			}
		} finally {
			pendingAction = ModelAction.NONE;
			restoringToState = NO_STATE_ID;
		}
	}

	public RuntimeException pendingActionError(String currentActionName) {
		var pendingActionName = switch (pendingAction) {
			case NONE -> throw new IllegalArgumentException("Trying to throw pending action error when there is no " +
					"pending action");
			case COMMIT -> "commit";
			case RESTORE -> "restore to %d".formatted(restoringToState);
		};
		return new IllegalStateException("Cannot %s due to pending %s".formatted(currentActionName, pendingActionName));
	}

	@Override
	public <T extends ModelAdapter> Optional<T> tryGetAdapter(ModelAdapterType<? extends T, ?, ?> adapterType) {
		return adapters.tryGet(adapterType, adapterType.getModelAdapterClass());
	}

	@Override
	public <T extends ModelAdapter> T getAdapter(ModelAdapterType<T, ?, ?> adapterType) {
		return adapters.get(adapterType, adapterType.getModelAdapterClass());
	}

	void addAdapter(AnyModelAdapterType adapterType, ModelAdapter adapter) {
		adapters.add(adapterType, adapter);
	}

	@Override
	public void addListener(ModelListener listener) {
		listeners.add(listener);
	}

	@Override
	public void removeListener(ModelListener listener) {
		listeners.remove(listener);
	}
}
