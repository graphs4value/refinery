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
import tools.refinery.store.query.view.AnySymbolView;
import tools.refinery.store.query.view.SymbolView;

import java.util.HashMap;
import java.util.Map;

public class ModelUpdateListener {
	private final Map<AnySymbolView, SymbolViewUpdateListener<?>> symbolViewUpdateListeners;

	public ModelUpdateListener(ViatraModelQueryAdapterImpl adapter) {
		var symbolViews = adapter.getStoreAdapter().getInputKeys().keySet();
		symbolViewUpdateListeners = new HashMap<>(symbolViews.size());
		for (var symbolView : symbolViews) {
			registerView(adapter, (SymbolView<?>) symbolView);
		}
	}

	private <T> void registerView(ViatraModelQueryAdapterImpl adapter, SymbolView<T> view) {
		var model = adapter.getModel();
		var interpretation = model.getInterpretation(view.getSymbol());
		var listener = SymbolViewUpdateListener.of(adapter, view, interpretation);
		symbolViewUpdateListeners.put(view, listener);
	}

	public boolean containsSymbolView(AnySymbolView relationView) {
		return symbolViewUpdateListeners.containsKey(relationView);
	}

	public void addListener(IInputKey key, AnySymbolView symbolView, ITuple seed,
							IQueryRuntimeContextListener listener) {
		var symbolViewUpdateListener = symbolViewUpdateListeners.get(symbolView);
		symbolViewUpdateListener.addFilter(key, seed, listener);
	}

	public void removeListener(IInputKey key, AnySymbolView symbolView, ITuple seed,
							   IQueryRuntimeContextListener listener) {
		var symbolViewUpdateListener = symbolViewUpdateListeners.get(symbolView);
		symbolViewUpdateListener.removeFilter(key, seed, listener);
	}
}
