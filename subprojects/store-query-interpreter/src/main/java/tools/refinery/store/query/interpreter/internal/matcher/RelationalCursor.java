/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.query.interpreter.internal.matcher;

import tools.refinery.interpreter.matchers.tuple.ITuple;
import tools.refinery.store.map.Cursor;
import tools.refinery.store.tuple.Tuple;

import java.util.Iterator;

class RelationalCursor implements Cursor<Tuple, Boolean> {
    private final Iterator<? extends ITuple> tuplesIterator;
    private boolean terminated;
    private Tuple key;

    public RelationalCursor(Iterator<? extends ITuple> tuplesIterator) {
        this.tuplesIterator = tuplesIterator;
    }

    @Override
    public Tuple getKey() {
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
            key = MatcherUtils.toRefineryTuple(tuplesIterator.next());
            return true;
        }
        terminated = true;
        return false;
    }
}
