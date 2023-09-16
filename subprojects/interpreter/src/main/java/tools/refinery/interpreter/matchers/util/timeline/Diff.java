/*******************************************************************************
 * Copyright (c) 2010-2019, Tamas Szabo, itemis AG, Gabor Bergmann, IncQuery Labs Ltd.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.interpreter.matchers.util.timeline;

import java.util.ArrayList;

import tools.refinery.interpreter.matchers.util.Signed;

/**
 * The description of a delta that specifies how a {@link Timeline} changes. It consists of {@link Signed} timestamps that
 * depict the moments of insertions and deletions on the timeline.
 *
 * @author Tamas Szabo
 * @since 2.4
 * @param <Timestamp>
 *            the type representing the timestamps
 */
public class Diff<Timestamp extends Comparable<Timestamp>> extends ArrayList<Signed<Timestamp>> {

    private static final long serialVersionUID = 3853460426655994160L;

    public Diff() {

    }

    public void appendWithCancellation(Signed<Timestamp> item) {
        if (this.isEmpty()) {
            this.add(item);
        } else {
            final Signed<Timestamp> last = this.get(this.size() - 1);
            final int lastMinusItem = last.getPayload().compareTo(item.getPayload());
            if (lastMinusItem == 0) {
                if (last.getDirection() != item.getDirection()) {
                    // cancellation
                    this.remove(this.size() - 1);
                } else {
                    throw new IllegalStateException(
                            "Trying to insert or delete for the second time at the same timestamp! " + item);
                }
            } else if (lastMinusItem > 0) {
                throw new IllegalStateException(
                        "Trying to append a timestamp that is smaller than the last one! " + last + " " + item);
            } else {
                this.add(item);
            }
        }
    }

}
