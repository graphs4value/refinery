/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.query.viatra.internal.update;

import org.eclipse.viatra.query.runtime.matchers.context.IInputKey;
import org.eclipse.viatra.query.runtime.matchers.context.IQueryRuntimeContextListener;
import org.eclipse.viatra.query.runtime.matchers.tuple.ITuple;
import tools.refinery.store.query.viatra.internal.ViatraModelQueryAdapterImpl;
import tools.refinery.store.query.view.AnyRelationView;
import tools.refinery.store.query.view.RelationView;

import java.util.HashMap;
import java.util.Map;

public class ModelUpdateListener {
	private final Map<AnyRelationView, RelationViewUpdateListener<?>> relationViewUpdateListeners;

	public ModelUpdateListener(ViatraModelQueryAdapterImpl adapter) {
		var relationViews = adapter.getStoreAdapter().getInputKeys().keySet();
		relationViewUpdateListeners = new HashMap<>(relationViews.size());
		for (var relationView : relationViews) {
			registerView(adapter, (RelationView<?>) relationView);
		}
	}

	private <T> void registerView(ViatraModelQueryAdapterImpl adapter, RelationView<T> relationView) {
		var model = adapter.getModel();
		var interpretation = model.getInterpretation(relationView.getSymbol());
		var listener = RelationViewUpdateListener.of(adapter, relationView, interpretation);
		relationViewUpdateListeners.put(relationView, listener);
	}

	public boolean containsRelationView(AnyRelationView relationView) {
		return relationViewUpdateListeners.containsKey(relationView);
	}

	public void addListener(IInputKey key, AnyRelationView relationView, ITuple seed,
							IQueryRuntimeContextListener listener) {
		var relationViewUpdateListener = relationViewUpdateListeners.get(relationView);
		relationViewUpdateListener.addFilter(key, seed, listener);
	}

	public void removeListener(IInputKey key, AnyRelationView relationView, ITuple seed,
							   IQueryRuntimeContextListener listener) {
		var relationViewUpdateListener = relationViewUpdateListeners.get(relationView);
		relationViewUpdateListener.removeFilter(key, seed, listener);
	}
}
