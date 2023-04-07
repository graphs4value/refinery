package tools.refinery.store.query.viatra.internal.matcher;

import org.eclipse.viatra.query.runtime.matchers.backend.IQueryResultProvider;
import org.eclipse.viatra.query.runtime.matchers.tuple.TupleMask;
import org.eclipse.viatra.query.runtime.matchers.tuple.Tuples;
import org.eclipse.viatra.query.runtime.rete.index.IterableIndexer;
import org.eclipse.viatra.query.runtime.rete.matcher.RetePatternMatcher;
import tools.refinery.store.map.Cursor;
import tools.refinery.store.query.ModelQueryAdapter;
import tools.refinery.store.query.ResultSet;
import tools.refinery.store.query.dnf.FunctionalQuery;
import tools.refinery.store.query.dnf.Query;
import tools.refinery.store.query.viatra.internal.ViatraModelQueryAdapterImpl;
import tools.refinery.store.tuple.Tuple;

/**
 * Directly access the tuples inside a VIATRA pattern matcher.<p>
 * This class neglects calling
 * {@link org.eclipse.viatra.query.runtime.matchers.context.IQueryRuntimeContext#wrapTuple(org.eclipse.viatra.query.runtime.matchers.tuple.Tuple)}
 * and
 * {@link org.eclipse.viatra.query.runtime.matchers.context.IQueryRuntimeContext#unwrapTuple(org.eclipse.viatra.query.runtime.matchers.tuple.Tuple)},
 * because {@link tools.refinery.store.query.viatra.internal.context.RelationalRuntimeContext} provides a trivial
 * implementation for these methods.
 * Using this class with any other runtime context may lead to undefined behavior.
 */
public class FunctionalViatraMatcher<T> implements ResultSet<T> {
	private final ViatraModelQueryAdapterImpl adapter;
	private final FunctionalQuery<T> query;
	private final TupleMask emptyMask;
	private final TupleMask omitOutputMask;
	private final IQueryResultProvider backend;
	private final IterableIndexer omitOutputIndexer;

	public FunctionalViatraMatcher(ViatraModelQueryAdapterImpl adapter, FunctionalQuery<T> query,
								   RawPatternMatcher rawPatternMatcher) {
		this.adapter = adapter;
		this.query = query;
		int arity = query.arity();
		int arityWithOutput = arity + 1;
		emptyMask = TupleMask.empty(arityWithOutput);
		omitOutputMask = TupleMask.omit(arity, arityWithOutput);
		backend = rawPatternMatcher.getBackend();
		if (backend instanceof RetePatternMatcher reteBackend) {
			var maybeIterableOmitOutputIndexer = IndexerUtils.getIndexer(reteBackend, omitOutputMask);
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
	public ModelQueryAdapter getAdapter() {
		return adapter;
	}

	@Override
	public Query<T> getQuery() {
		return query;
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
}
