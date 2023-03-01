package tools.refinery.store.query.viatra.internal.pquery;

import org.eclipse.viatra.query.runtime.api.GenericPatternMatcher;
import org.eclipse.viatra.query.runtime.api.GenericQuerySpecification;
import org.eclipse.viatra.query.runtime.api.ViatraQueryEngine;
import org.eclipse.viatra.query.runtime.matchers.backend.IMatcherCapability;
import org.eclipse.viatra.query.runtime.matchers.backend.IQueryResultProvider;
import org.eclipse.viatra.query.runtime.matchers.tuple.TupleMask;
import org.eclipse.viatra.query.runtime.matchers.tuple.Tuples;
import org.eclipse.viatra.query.runtime.rete.index.Indexer;
import org.eclipse.viatra.query.runtime.rete.matcher.RetePatternMatcher;
import tools.refinery.store.map.Cursor;
import tools.refinery.store.map.Cursors;
import tools.refinery.store.query.ResultSet;
import tools.refinery.store.query.viatra.ViatraTupleLike;
import tools.refinery.store.tuple.Tuple;
import tools.refinery.store.tuple.TupleLike;

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
public class RawPatternMatcher extends GenericPatternMatcher implements ResultSet {
    private final Object[] empty;
    private final TupleMask identityMask;
    private Indexer emptyMaskIndexer;

    public RawPatternMatcher(GenericQuerySpecification<? extends GenericPatternMatcher> specification) {
        super(specification);
        var arity = specification.getParameterNames().size();
        empty = new Object[arity];
        identityMask = TupleMask.identity(arity);
    }

    @Override
    protected void setBackend(ViatraQueryEngine engine, IQueryResultProvider resultProvider,
                              IMatcherCapability capabilities) {
        super.setBackend(engine, resultProvider, capabilities);
        if (resultProvider instanceof RetePatternMatcher reteBackend) {
            emptyMaskIndexer = IndexerUtils.getIndexer(reteBackend, TupleMask.empty(identityMask.sourceWidth));
        }
    }

    @Override
    public boolean hasResult(TupleLike parameters) {
        org.eclipse.viatra.query.runtime.matchers.tuple.Tuple tuple;
        if (parameters instanceof ViatraTupleLike viatraTupleLike) {
            tuple = viatraTupleLike.wrappedTuple().toImmutable();
        } else {
            var parametersArray = toParametersArray(parameters);
            tuple = Tuples.flatTupleOf(parametersArray);
        }
        if (emptyMaskIndexer == null) {
            return backend.hasMatch(identityMask, tuple);
        }
        var matches = emptyMaskIndexer.get(Tuples.staticArityFlatTupleOf());
        return matches != null && matches.contains(tuple);
    }

    @Override
    public Cursor<TupleLike, Boolean> allResults() {
        if (emptyMaskIndexer == null) {
            return new ResultSetCursor(backend.getAllMatches(empty).iterator());
        }
        var matches = emptyMaskIndexer.get(Tuples.staticArityFlatTupleOf());
        return matches == null ? Cursors.empty() : new ResultSetCursor(matches.stream().iterator());
    }

    @Override
    public int countResults() {
        if (emptyMaskIndexer == null) {
            return backend.countMatches(empty);
        }
        var matches = emptyMaskIndexer.get(Tuples.staticArityFlatTupleOf());
        return matches == null ? 0 : matches.size();
    }

    private Object[] toParametersArray(TupleLike tuple) {
        int size = tuple.getSize();
        var array = new Object[size];
        for (int i = 0; i < size; i++) {
            var value = tuple.get(i);
            array[i] = Tuple.of(value);
        }
        return array;
    }
}
