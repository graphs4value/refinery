/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.query.interpreter.internal.context;

import tools.refinery.interpreter.matchers.context.AbstractQueryMetaContext;
import tools.refinery.interpreter.matchers.context.IInputKey;
import tools.refinery.interpreter.matchers.context.InputKeyImplication;
import tools.refinery.interpreter.matchers.context.common.JavaTransitiveInstancesKey;
import tools.refinery.store.query.interpreter.internal.pquery.SymbolViewWrapper;
import tools.refinery.store.query.view.AnySymbolView;

import java.util.*;

/**
 * The meta context information for String scopes.
 */
public class RelationalQueryMetaContext extends AbstractQueryMetaContext {
	private final Map<AnySymbolView, IInputKey> inputKeys;

	RelationalQueryMetaContext(Map<AnySymbolView, IInputKey> inputKeys) {
		this.inputKeys = inputKeys;
	}

	@Override
	public boolean isEnumerable(IInputKey key) {
		checkKey(key);
		return key.isEnumerable();
	}

	@Override
	public boolean isStateless(IInputKey key) {
		checkKey(key);
		return true;
	}

	@Override
	public boolean canLeadOutOfScope(IInputKey key) {
		return false;
	}

	@Override
	public Collection<InputKeyImplication> getImplications(IInputKey implyingKey) {
		if (implyingKey instanceof JavaTransitiveInstancesKey) {
			return List.of();
		}
		var symbolView = checkKey(implyingKey);
		var relationViewImplications = symbolView.getImpliedRelationViews();
		var inputKeyImplications = new HashSet<InputKeyImplication>(relationViewImplications.size());
		for (var relationViewImplication : relationViewImplications) {
			if (!symbolView.equals(relationViewImplication.implyingView())) {
				throw new IllegalArgumentException("Relation view %s returned unrelated implication %s".formatted(
						symbolView, relationViewImplication));
			}
			var impliedInputKey = inputKeys.get(relationViewImplication.impliedView());
			// Ignore implications not relevant for any queries included in the model.
			if (impliedInputKey != null) {
				inputKeyImplications.add(new InputKeyImplication(implyingKey, impliedInputKey,
						relationViewImplication.impliedIndices()));
			}
		}
		var parameters = symbolView.getParameters();
		int arity = symbolView.arity();
		for (int i = 0; i < arity; i++) {
			var parameter = parameters.get(i);
			var parameterType = parameter.tryGetType();
			if (parameterType.isPresent()) {
				var javaTransitiveInstancesKey = new JavaTransitiveInstancesKey(parameterType.get());
				var javaImplication = new InputKeyImplication(implyingKey, javaTransitiveInstancesKey, List.of(i));
				inputKeyImplications.add(javaImplication);
			}
		}
		return inputKeyImplications;
	}

	@Override
	public Map<Set<Integer>, Set<Integer>> getFunctionalDependencies(IInputKey key) {
		if (key instanceof JavaTransitiveInstancesKey) {
			return Map.of();
		}
		var relationView = checkKey(key);
		var functionalDependencies = relationView.getFunctionalDependencies();
		var flattened = new HashMap<Set<Integer>, Set<Integer>>(functionalDependencies.size());
		for (var functionalDependency : functionalDependencies) {
			var forEach = functionalDependency.forEach();
			checkValidIndices(relationView, forEach);
			var unique = functionalDependency.unique();
			checkValidIndices(relationView, unique);
			var existing = flattened.get(forEach);
			if (existing == null) {
				flattened.put(forEach, new HashSet<>(unique));
			} else {
				existing.addAll(unique);
			}
		}
		return flattened;
	}

	private static void checkValidIndices(AnySymbolView relationView, Collection<Integer> indices) {
		indices.stream().filter(relationView::invalidIndex).findAny().ifPresent(i -> {
			throw new IllegalArgumentException("Index %d is invalid for %s".formatted(i, relationView));
		});
	}

	public AnySymbolView checkKey(IInputKey key) {
		if (!(key instanceof SymbolViewWrapper wrapper)) {
			throw new IllegalArgumentException("The input key %s is not a valid input key".formatted(key));
		}
		var symbolView = wrapper.getWrappedKey();
		if (!inputKeys.containsKey(symbolView)) {
			throw new IllegalArgumentException("The input key %s is not present in the model".formatted(key));
		}
		return symbolView;
	}
}
