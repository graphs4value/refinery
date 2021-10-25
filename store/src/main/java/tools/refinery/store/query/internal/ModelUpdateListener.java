package tools.refinery.store.query.internal;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.viatra.query.runtime.matchers.context.IQueryRuntimeContextListener;
import org.eclipse.viatra.query.runtime.matchers.tuple.ITuple;

import tools.refinery.store.model.Tuple;
import tools.refinery.store.model.representation.Relation;
import tools.refinery.store.query.view.RelationView;

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
		var views = relation2View.get(relation);
		if (views == null) {
			views = new HashSet<>();
			relation2View.put(relation, views);
		}
		views.add(view);

		// 2. register notifier map to views, if necessary
		if (!view2Buffers.containsKey(view)) {
			view2Buffers.put(view, new HashSet<>());
		}
	}

	boolean containsRelationalView(RelationView<?> relationalKey) {
		return view2Buffers.containsKey(relationalKey);
	}

	<D> void addListener(RelationView<D> relationView, ITuple seed, IQueryRuntimeContextListener listener) {
		if (view2Buffers.containsKey(relationView)) {
			ViewUpdateTranslator<D> updateListener = new ViewUpdateTranslator<>(relationView, seed, listener);
			ViewUpdateBuffer<D> updateBuffer = new ViewUpdateBuffer<>(updateListener);
			view2Buffers.get(relationView).add(updateBuffer);
		} else
			throw new IllegalArgumentException();
	}

	void removeListener(RelationView<?> relationView, ITuple seed, IQueryRuntimeContextListener listener) {
		if (view2Buffers.containsKey(relationView)) {
			Set<ViewUpdateBuffer<?>> buffers = this.view2Buffers.get(relationView);
			for(var buffer : buffers) {
				if(buffer.getUpdateListener().key == seed && buffer.getUpdateListener().listener == listener) {
					// remove buffer and terminate immediately, or it will break iterator.
					buffers.remove(buffer);
					return;
				}
			}
		} else
			throw new IllegalArgumentException();
	}

	public <D> void addUpdate(Relation<D> relation, Tuple key, D oldValue, D newValue) {
		var views = this.relation2View.get(relation);
		if (views != null) {
			for (var view : views) {
				var buffers = this.view2Buffers.get(view);
				for (var buffer : buffers) {
					@SuppressWarnings("unchecked")
					var typedBuffer = (ViewUpdateBuffer<D>) buffer;
					typedBuffer.addChange(key, oldValue, newValue);
				}
			}
		}
	}

	public boolean hasChange() {
		for (var bufferCollection : this.view2Buffers.values()) {
			for (ViewUpdateBuffer<?> buffer : bufferCollection) {
				if (buffer.hasChange())
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
