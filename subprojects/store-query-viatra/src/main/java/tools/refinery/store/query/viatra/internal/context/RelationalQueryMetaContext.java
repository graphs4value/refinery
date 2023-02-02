package tools.refinery.store.query.viatra.internal.context;

import org.eclipse.viatra.query.runtime.matchers.context.AbstractQueryMetaContext;
import org.eclipse.viatra.query.runtime.matchers.context.IInputKey;
import org.eclipse.viatra.query.runtime.matchers.context.InputKeyImplication;
import tools.refinery.store.query.viatra.internal.pquery.RelationViewWrapper;
import tools.refinery.store.query.view.AnyRelationView;

import java.util.*;

/**
 * The meta context information for String scopes.
 */
public class RelationalQueryMetaContext extends AbstractQueryMetaContext {
	private final Map<AnyRelationView, IInputKey> inputKeys;

	RelationalQueryMetaContext(Map<AnyRelationView, IInputKey> inputKeys) {
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
		var relationView = checkKey(implyingKey);
		var relationViewImplications = relationView.getImpliedRelationViews();
		var inputKeyImplications = new HashSet<InputKeyImplication>(relationViewImplications.size());
		for (var relationViewImplication : relationViewImplications) {
			if (!relationView.equals(relationViewImplication.implyingRelationView())) {
				throw new IllegalArgumentException("Relation view %s returned unrelated implication %s".formatted(
						relationView, relationViewImplication));
			}
			var impliedInputKey = inputKeys.get(relationViewImplication.impliedRelationView());
			// Ignore implications not relevant for any queries included in the model.
			if (impliedInputKey != null) {
				inputKeyImplications.add(new InputKeyImplication(implyingKey, impliedInputKey,
						relationViewImplication.impliedIndices()));
			}
		}
		return inputKeyImplications;
	}

	@Override
	public Map<Set<Integer>, Set<Integer>> getFunctionalDependencies(IInputKey key) {
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

	private static void checkValidIndices(AnyRelationView relationView, Collection<Integer> indices) {
		indices.stream().filter(relationView::invalidIndex).findAny().ifPresent(i -> {
			throw new IllegalArgumentException("Index %d is invalid for %s".formatted(i, relationView));
		});
	}

	public AnyRelationView checkKey(IInputKey key) {
		if (!(key instanceof RelationViewWrapper wrapper)) {
			throw new IllegalArgumentException("The input key %s is not a valid input key".formatted(key));
		}
		var relationView = wrapper.getWrappedKey();
		if (!inputKeys.containsKey(relationView)) {
			throw new IllegalArgumentException("The input key %s is not present in the model".formatted(key));
		}
		return relationView;
	}
}
