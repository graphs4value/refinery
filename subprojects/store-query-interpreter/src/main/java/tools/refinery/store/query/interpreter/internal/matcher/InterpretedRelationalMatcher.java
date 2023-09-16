/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.query.interpreter.internal.matcher;

import tools.refinery.interpreter.matchers.context.IQueryRuntimeContext;
import tools.refinery.interpreter.matchers.tuple.TupleMask;
import tools.refinery.interpreter.matchers.tuple.Tuples;
import tools.refinery.interpreter.rete.index.Indexer;
import tools.refinery.interpreter.rete.matcher.RetePatternMatcher;
import tools.refinery.store.map.Cursor;
import tools.refinery.store.map.Cursors;
import tools.refinery.store.query.dnf.RelationalQuery;
import tools.refinery.store.query.interpreter.internal.QueryInterpreterAdapterImpl;
import tools.refinery.store.tuple.Tuple;

/**
 * Directly access the tuples inside a Refinery Interpreter pattern matcher.<p>
 * This class neglects calling
 * {@link IQueryRuntimeContext#wrapTuple(tools.refinery.interpreter.matchers.tuple.Tuple)}
 * and
 * {@link IQueryRuntimeContext#unwrapTuple(tools.refinery.interpreter.matchers.tuple.Tuple)},
 * because {@link tools.refinery.store.query.interpreter.internal.context.RelationalRuntimeContext} provides a trivial
 * implementation for these methods.
 * Using this class with any other runtime context may lead to undefined behavior.
 */
public class InterpretedRelationalMatcher extends AbstractInterpretedMatcher<Boolean> {
	private final TupleMask emptyMask;
	private final TupleMask identityMask;
	private final Indexer emptyMaskIndexer;

	public InterpretedRelationalMatcher(QueryInterpreterAdapterImpl adapter, RelationalQuery query,
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
	public void update(tools.refinery.interpreter.matchers.tuple.Tuple updateElement, boolean isInsertion) {
		var key = MatcherUtils.toRefineryTuple(updateElement);
		notifyChange(key, !isInsertion, isInsertion);
	}
}
