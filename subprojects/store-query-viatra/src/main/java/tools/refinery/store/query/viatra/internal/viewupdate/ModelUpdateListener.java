package tools.refinery.store.query.viatra.internal.viewupdate;

import org.eclipse.viatra.query.runtime.matchers.context.IInputKey;
import org.eclipse.viatra.query.runtime.matchers.context.IQueryRuntimeContextListener;
import org.eclipse.viatra.query.runtime.matchers.tuple.ITuple;
import tools.refinery.store.tuple.Tuple;
import tools.refinery.store.model.representation.Relation;
import tools.refinery.store.query.view.RelationView;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ModelUpdateListener {
	/**
	 * Collections of Relations and their Views.
	 */
	private final Map<Relation<?>, Set<RelationView<?>>> relation2View;

	/**
	 * Collection of Views and their buffers.
	 */
	private final Map<RelationView<?>, Set<ViewUpdateBuffer<?>>> view2Buffers;

	public ModelUpdateListener(Set<RelationView<?>> relationViews) {
		this.relation2View = new HashMap<>();
		this.view2Buffers = new HashMap<>();

		for (RelationView<?> relationView : relationViews) {
			registerView(relationView);
		}
	}

	private void registerView(RelationView<?> view) {
		Relation<?> relation = view.getRepresentation();

		// 1. register views to relations, if necessary
		var views = relation2View.computeIfAbsent(relation, x -> new HashSet<>());
		views.add(view);

		// 2. register notifier map to views, if necessary
		view2Buffers.computeIfAbsent(view, x -> new HashSet<>());
	}

	public boolean containsRelationalView(RelationView<?> relationalKey) {
		return view2Buffers.containsKey(relationalKey);
	}

	public <D> void addListener(IInputKey key, RelationView<D> relationView, ITuple seed,
								IQueryRuntimeContextListener listener) {
		if (view2Buffers.containsKey(relationView)) {
			ViewUpdateTranslator<D> updateListener = new ViewUpdateTranslator<>(key, relationView, seed, listener);
			ViewUpdateBuffer<D> updateBuffer = new ViewUpdateBuffer<>(updateListener);
			view2Buffers.get(relationView).add(updateBuffer);
		} else {
			throw new IllegalArgumentException();
		}
	}

	public void removeListener(IInputKey key, RelationView<?> relationView, ITuple seed,
							   IQueryRuntimeContextListener listener) {
		if (view2Buffers.containsKey(relationView)) {
			Set<ViewUpdateBuffer<?>> buffers = this.view2Buffers.get(relationView);
			for (var buffer : buffers) {
				if (buffer.getUpdateListener().equals(key, relationView, seed, listener)) {
					// remove buffer and terminate immediately, or it will break iterator.
					buffers.remove(buffer);
					return;
				}
			}
		} else {
			throw new IllegalArgumentException("Relation view is not registered for updates");
		}
	}

	public <D> void addUpdate(Relation<D> relation, Tuple key, D oldValue, D newValue) {
		var views = this.relation2View.get(relation);
		if (views == null) {
			return;
		}
		for (var view : views) {
			var buffers = this.view2Buffers.get(view);
			for (var buffer : buffers) {
				@SuppressWarnings("unchecked")
				var typedBuffer = (ViewUpdateBuffer<D>) buffer;
				typedBuffer.addChange(key, oldValue, newValue);
			}
		}
	}

	public boolean hasChanges() {
		for (var bufferCollection : this.view2Buffers.values()) {
			for (ViewUpdateBuffer<?> buffer : bufferCollection) {
				if (buffer.hasChanges())
					return true;
			}
		}
		return false;
	}

	public void flush() {
		for (var bufferCollection : this.view2Buffers.values()) {
			for (ViewUpdateBuffer<?> buffer : bufferCollection) {
				buffer.flush();
			}
		}
	}
}
