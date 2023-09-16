/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.query.interpreter.internal.matcher;

import tools.refinery.interpreter.matchers.context.IQueryRuntimeContext;
import tools.refinery.interpreter.matchers.tuple.TupleMask;
import tools.refinery.interpreter.matchers.tuple.Tuples;
import tools.refinery.interpreter.rete.index.IterableIndexer;
import tools.refinery.interpreter.rete.matcher.RetePatternMatcher;
import tools.refinery.store.map.Cursor;
import tools.refinery.store.query.dnf.FunctionalQuery;
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
public class InterpretedFunctionalMatcher<T> extends AbstractInterpretedMatcher<T> {
	private final TupleMask emptyMask;
	private final TupleMask omitOutputMask;
	private final IterableIndexer omitOutputIndexer;

	public InterpretedFunctionalMatcher(QueryInterpreterAdapterImpl adapter, FunctionalQuery<T> query,
										RawPatternMatcher rawPatternMatcher) {
		super(adapter, query, rawPatternMatcher);
		int arity = query.arity();
		int arityWithOutput = arity + 1;
		emptyMask = TupleMask.empty(arityWithOutput);
		omitOutputMask = TupleMask.omit(arity, arityWithOutput);
		if (backend instanceof RetePatternMatcher reteBackend) {
			var maybeIterableOmitOutputIndexer = reteBackend.getInternalIndexer(omitOutputMask);
			if (maybeIterableOmitOutputIndexer instanceof IterableIndexer iterableOmitOutputIndexer) {
				omitOutputIndexer = iterableOmitOutputIndexer;
			} else {
				omitOutputIndexer = null;
			}
		} else {
			omitOutputIndexer = null;
		}
	}

	@Override
	public T get(Tuple parameters) {
		var tuple = MatcherUtils.toViatraTuple(parameters);
		if (omitOutputIndexer == null) {
			return MatcherUtils.getSingleValue(backend.getAllMatches(omitOutputMask, tuple).iterator());
		} else {
			return MatcherUtils.getSingleValue(omitOutputIndexer.get(tuple));
		}
	}

	@Override
	public Cursor<Tuple, T> getAll() {
		if (omitOutputIndexer == null) {
			var allMatches = backend.getAllMatches(emptyMask, Tuples.staticArityFlatTupleOf());
			return new UnsafeFunctionalCursor<>(allMatches.iterator());
		}
		return new FunctionalCursor<>(omitOutputIndexer);
	}

	@Override
	public int size() {
		if (omitOutputIndexer == null) {
			return backend.countMatches(emptyMask, Tuples.staticArityFlatTupleOf());
		}
		return omitOutputIndexer.getBucketCount();
	}

	@Override
	public void update(tools.refinery.interpreter.matchers.tuple.Tuple updateElement, boolean isInsertion) {
		var key = MatcherUtils.keyToRefineryTuple(updateElement);
		var value = MatcherUtils.<T>getValue(updateElement);
		if (isInsertion) {
			notifyChange(key, null, value);
		} else {
			notifyChange(key, value, null);
		}
	}
}
