/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.query.viatra.internal.matcher;

import tools.refinery.viatra.runtime.matchers.tuple.TupleMask;
import tools.refinery.viatra.runtime.matchers.tuple.Tuples;
import tools.refinery.viatra.runtime.rete.index.Indexer;
import tools.refinery.viatra.runtime.rete.matcher.RetePatternMatcher;
import tools.refinery.store.map.Cursor;
import tools.refinery.store.map.Cursors;
import tools.refinery.store.query.dnf.RelationalQuery;
import tools.refinery.store.query.viatra.internal.ViatraModelQueryAdapterImpl;
import tools.refinery.store.tuple.Tuple;

/**
 * Directly access the tuples inside a VIATRA pattern matcher.<p>
 * This class neglects calling
 * {@link tools.refinery.viatra.runtime.matchers.context.IQueryRuntimeContext#wrapTuple(org.eclipse.viatra.query.runtime.matchers.tuple.Tuple)}
 * and
 * {@link tools.refinery.viatra.runtime.matchers.context.IQueryRuntimeContext#unwrapTuple(org.eclipse.viatra.query.runtime.matchers.tuple.Tuple)},
 * because {@link tools.refinery.store.query.viatra.internal.context.RelationalRuntimeContext} provides a trivial
 * implementation for these methods.
 * Using this class with any other runtime context may lead to undefined behavior.
 */
public class RelationalViatraMatcher extends AbstractViatraMatcher<Boolean> {
	private final TupleMask emptyMask;
	private final TupleMask identityMask;
	private final Indexer emptyMaskIndexer;

	public RelationalViatraMatcher(ViatraModelQueryAdapterImpl adapter, RelationalQuery query,
								   RawPatternMatcher rawPatternMatcher) {
		super(adapter, query, rawPatternMatcher);
		int arity = query.arity();
		emptyMask = TupleMask.empty(arity);
		identityMask = TupleMask.identity(arity);
		if (backend instanceof RetePatternMatcher reteBackend) {
			emptyMaskIndexer = reteBackend.getInternalIndexer(emptyMask);
		} else {
			emptyMaskIndexer = null;
		}
	}

	@Override
	public Boolean get(Tuple parameters) {
		var tuple = MatcherUtils.toViatraTuple(parameters);
		if (emptyMaskIndexer == null) {
			return backend.hasMatch(identityMask, tuple);
		}
		var matches = emptyMaskIndexer.get(Tuples.staticArityFlatTupleOf());
		return matches != null && matches.contains(tuple);
	}

	@Override
	public Cursor<Tuple, Boolean> getAll() {
		if (emptyMaskIndexer == null) {
			var allMatches = backend.getAllMatches(emptyMask, Tuples.staticArityFlatTupleOf());
			return new RelationalCursor(allMatches.iterator());
		}
		var matches = emptyMaskIndexer.get(Tuples.staticArityFlatTupleOf());
		return matches == null ? Cursors.empty() : new RelationalCursor(matches.stream().iterator());
	}

	@Override
	public int size() {
		if (emptyMaskIndexer == null) {
			return backend.countMatches(emptyMask, Tuples.staticArityFlatTupleOf());
		}
		var matches = emptyMaskIndexer.get(Tuples.staticArityFlatTupleOf());
		return matches == null ? 0 : matches.size();
	}

	@Override
	public void update(tools.refinery.viatra.runtime.matchers.tuple.Tuple updateElement, boolean isInsertion) {
		var key = MatcherUtils.toRefineryTuple(updateElement);
		notifyChange(key, !isInsertion, isInsertion);
	}
}
