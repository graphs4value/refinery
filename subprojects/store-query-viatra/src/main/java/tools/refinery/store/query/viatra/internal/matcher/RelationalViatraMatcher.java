package tools.refinery.store.query.viatra.internal.matcher;

import org.eclipse.viatra.query.runtime.matchers.backend.IQueryResultProvider;
import org.eclipse.viatra.query.runtime.matchers.tuple.TupleMask;
import org.eclipse.viatra.query.runtime.matchers.tuple.Tuples;
import org.eclipse.viatra.query.runtime.rete.index.Indexer;
import org.eclipse.viatra.query.runtime.rete.matcher.RetePatternMatcher;
import tools.refinery.store.map.Cursor;
import tools.refinery.store.map.Cursors;
import tools.refinery.store.query.ModelQueryAdapter;
import tools.refinery.store.query.ResultSet;
import tools.refinery.store.query.dnf.Query;
import tools.refinery.store.query.dnf.RelationalQuery;
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
public class RelationalViatraMatcher implements ResultSet<Boolean> {
	private final ViatraModelQueryAdapterImpl adapter;
	private final RelationalQuery query;
	private final TupleMask emptyMask;
	private final TupleMask identityMask;
	private final IQueryResultProvider backend;
	private final Indexer emptyMaskIndexer;

	public RelationalViatraMatcher(ViatraModelQueryAdapterImpl adapter, RelationalQuery query,
								   RawPatternMatcher rawPatternMatcher) {
		this.adapter = adapter;
		this.query = query;
		int arity = query.arity();
		emptyMask = TupleMask.empty(arity);
		identityMask = TupleMask.identity(arity);
		backend = rawPatternMatcher.getBackend();
		if (backend instanceof RetePatternMatcher reteBackend) {
			emptyMaskIndexer = IndexerUtils.getIndexer(reteBackend, emptyMask);
		} else {
			emptyMaskIndexer = null;
		}
	}

	@Override
	public ModelQueryAdapter getAdapter() {
		return adapter;
	}

	@Override
	public Query<Boolean> getQuery() {
		return query;
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
}
