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
		var listener = RelationViewUpdateListener.of(adapter, relationView);
		var model = adapter.getModel();
		var interpretation = model.getInterpretation(relationView.getSymbol());
		interpretation.addListener(listener, true);
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
