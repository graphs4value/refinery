package tools.refinery.store.query.viatra.internal.context;

import org.eclipse.viatra.query.runtime.matchers.context.AbstractQueryMetaContext;
import org.eclipse.viatra.query.runtime.matchers.context.IInputKey;
import org.eclipse.viatra.query.runtime.matchers.context.InputKeyImplication;
import tools.refinery.store.query.view.RelationView;

import java.util.*;

/**
 * The meta context information for String scopes.
 */
public class RelationalQueryMetaContext extends AbstractQueryMetaContext {
	@Override
	public boolean isEnumerable(IInputKey key) {
		ensureValidKey(key);
		return key.isEnumerable();
	}

	@Override
	public boolean isStateless(IInputKey key) {
		ensureValidKey(key);
		return true;
	}

	@Override
	public Collection<InputKeyImplication> getImplications(IInputKey implyingKey) {
		ensureValidKey(implyingKey);
		return Set.of();
	}

	@Override
	public Map<Set<Integer>, Set<Integer>> getFunctionalDependencies(IInputKey key) {
		ensureValidKey(key);
		return Map.of();
	}

	public void ensureValidKey(IInputKey key) {
		if (key instanceof RelationView<?>) {
			return;
		}
		throw new IllegalArgumentException("The input key %s is not a valid input key.".formatted(key));
	}
}
