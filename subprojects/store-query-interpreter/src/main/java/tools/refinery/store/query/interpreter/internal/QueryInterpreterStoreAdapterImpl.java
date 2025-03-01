/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.query.interpreter.internal;

import tools.refinery.interpreter.CancellationToken;
import tools.refinery.interpreter.api.InterpreterEngineOptions;
import tools.refinery.interpreter.matchers.context.IInputKey;
import tools.refinery.logic.dnf.AnyQuery;
import tools.refinery.logic.dnf.Query;
import tools.refinery.store.model.Model;
import tools.refinery.store.model.ModelStore;
import tools.refinery.store.query.interpreter.QueryInterpreterStoreAdapter;
import tools.refinery.store.query.view.AnySymbolView;

import java.util.Collection;
import java.util.Map;

public class QueryInterpreterStoreAdapterImpl implements QueryInterpreterStoreAdapter {
	private final ModelStore store;
	private final InterpreterEngineOptions engineOptions;
	private final Map<AnySymbolView, IInputKey> inputKeys;
	private final ValidatedQueries validatedQueries;
	private final CancellationToken cancellationToken;

	QueryInterpreterStoreAdapterImpl(ModelStore store, InterpreterEngineOptions engineOptions,
									 Map<AnySymbolView, IInputKey> inputKeys,
									 ValidatedQueries validatedQueries,
									 CancellationToken cancellationToken) {
		this.store = store;
		this.engineOptions = engineOptions;
		this.inputKeys = inputKeys;
		this.validatedQueries = validatedQueries;
		this.cancellationToken = cancellationToken;
	}

	@Override
	public ModelStore getStore() {
		return store;
	}

	public Collection<AnySymbolView> getSymbolViews() {
		return inputKeys.keySet();
	}

	public Map<AnySymbolView, IInputKey> getInputKeys() {
		return inputKeys;
	}

	@Override
	public Collection<AnyQuery> getQueries() {
		return validatedQueries.getAllQueries();
	}

	public CancellationToken getCancellationToken() {
		return cancellationToken;
	}

	@Override
	public <T> Query<T> getCanonicalQuery(Query<T> query) {
		// We know that canonical forms of queries do not change output types.
		@SuppressWarnings("unchecked")
		var canonicalQuery = (Query<T>) validatedQueries.getCanonicalQueryMap().get(query);
		if (canonicalQuery == null) {
			throw new IllegalArgumentException("Unknown query: " + query);
		}
		return canonicalQuery;
	}

	ValidatedQueries getValidatedQueries() {
		return validatedQueries;
	}

	@Override
	public InterpreterEngineOptions getEngineOptions() {
		return engineOptions;
	}

	@Override
	public QueryInterpreterAdapterImpl createModelAdapter(Model model) {
		return new QueryInterpreterAdapterImpl(model, this);
	}
}
