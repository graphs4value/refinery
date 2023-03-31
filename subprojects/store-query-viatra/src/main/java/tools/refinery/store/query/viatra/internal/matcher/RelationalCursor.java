package tools.refinery.store.query.viatra.internal.matcher;

import org.eclipse.viatra.query.runtime.matchers.tuple.ITuple;
import tools.refinery.store.map.Cursor;
import tools.refinery.store.tuple.TupleLike;

import java.util.Iterator;

class RelationalCursor implements Cursor<TupleLike, Boolean> {
    private final Iterator<? extends ITuple> tuplesIterator;
    private boolean terminated;
    private TupleLike key;

    public RelationalCursor(Iterator<? extends ITuple> tuplesIterator) {
        this.tuplesIterator = tuplesIterator;
    }

    @Override
    public TupleLike getKey() {
        return key;
    }

    @Override
    public Boolean getValue() {
        return true;
    }

    @Override
    public boolean isTerminated() {
        return terminated;
    }

    @Override
    public boolean move() {
        if (!terminated && tuplesIterator.hasNext()) {
            key = new ViatraTupleLike(tuplesIterator.next());
            return true;
        }
        terminated = true;
        return false;
    }
}
