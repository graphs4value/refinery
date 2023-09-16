/*******************************************************************************
 * Copyright (c) 2010-2019, Tamas Szabo, itemis AG, Gabor Bergmann, IncQuery Labs Ltd.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.interpreter.matchers.util.timeline;

import java.util.Collections;

import tools.refinery.interpreter.matchers.util.Direction;
import tools.refinery.interpreter.matchers.util.Signed;

/**
 * A timeline which solely consists of one timestamp value, representing a single insertion. Intuitively, a singleton
 * timeline always represents a bump which starts at the given timestamp and lasts till plus infinity.
 *
 * @author Tamas Szabo
 * @since 2.4
 */
public class SingletonTimeline<Timestamp extends Comparable<Timestamp>> extends Timeline<Timestamp> {

    protected final Timestamp start;

    SingletonTimeline(final Timestamp timestamp) {
        this.start = timestamp;
    }

    SingletonTimeline(final Diff<Timestamp> diff) {
        if (diff.size() != 1 || diff.get(0).getDirection() == Direction.DELETE) {
            throw new IllegalArgumentException("There is only a single (insert) timestamp in the singleton timestamp!");
        } else {
            this.start = diff.get(0).getPayload();
        }
    }

    @Override
    public Signed<Timestamp> getSigned(final int index) {
        return new Signed<>(Direction.INSERT, this.getUnsigned(index));
    }

    @Override
    public Timestamp getUnsigned(final int index) {
        if (index != 0) {
            throw new IllegalArgumentException("There is only a single (insert) timestamp in the singleton timestamp!");
        } else {
            return this.start;
        }
    }

    @Override
    public int size() {
        return 1;
    }

    @Override
    public boolean isPresentAtInfinity() {
        return true;
    }

    @Override
    public Iterable<Signed<Timestamp>> asChangeSequence() {
        return Collections.singletonList(this.getSigned(0));
    }

    @Override
    public boolean isEmpty() {
        return false;
    }

}
