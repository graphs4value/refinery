/*******************************************************************************
 * Copyright (c) 2004-2008 Gabor Bergmann and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package tools.refinery.interpreter.rete.misc;

import java.util.Collection;
import java.util.LinkedHashSet;

import tools.refinery.interpreter.rete.network.ReteContainer;
import tools.refinery.interpreter.rete.network.communication.Timestamp;
import tools.refinery.interpreter.matchers.tuple.Tuple;
import tools.refinery.interpreter.matchers.util.Clearable;
import tools.refinery.interpreter.matchers.util.Direction;

/**
 * A monitoring object that connects to the rete network as a receiver to reflect changes since an arbitrary state
 * acknowledged by the client. Match tuples are represented by a type MatchType.
 *
 * <p>
 * <b>Usage</b>. If a new matching is found, it appears in the matchFoundEvents collection, and disappears when that
 * particular matching cannot be found anymore. If the event of finding a match has been processed by the client, it can
 * be removed manually. In this case, when a previously found matching is lost, the Tuple will appear in the
 * matchLostEvents collection, and disappear upon finding the same matching again. "Matching lost" events can also be
 * acknowledged by removing a Tuple from the collection. If the matching is found once again, it will return to
 * matchFoundEvents.
 *
 * <p>
 * <b>Technical notes</b>. Does NOT propagate updates!
 *
 * By overriding statelessConvert(), results can be stored to a MatchType. MatchType must provide equals() and
 * hashCode() reflecting its contents. The default implementation (DefaultDeltaMonitor) uses Tuple as MatchType.
 *
 * By overriding statelessFilter(), some tuples can be filtered.
 *
 * @author Gabor Bergmann
 *
 */
public abstract class DeltaMonitor<MatchType> extends SimpleReceiver implements Clearable {

    /**
     * matches that are newly found
     */
    public Collection<MatchType> matchFoundEvents;
    /**
     * matches that are newly lost
     */
    public Collection<MatchType> matchLostEvents;

    /**
     * @param reteContainer
     */
    public DeltaMonitor(ReteContainer reteContainer) {
        super(reteContainer);
        matchFoundEvents = new LinkedHashSet<MatchType>();
        matchLostEvents = new LinkedHashSet<MatchType>();
        reteContainer.registerClearable(this);
    }

    // /**
    // * Build a delta monitor into the head container of the network.
    // *
    // * @param network
    // */
    // public DeltaMonitor(Network network) {
    // this(network.getHeadContainer());
    // }

    /**
     * Override this method to provide a lightweight, stateless filter on the tuples
     *
     * @param tuple
     *            the occurrence that is to be filtered
     * @return true if this tuple should be monitored, false if ignored
     */
    public boolean statelessFilter(Tuple tuple) {
        return true;
    }

    public abstract MatchType statelessConvert(Tuple tuple);

    @Override
    public void update(Direction direction, Tuple updateElement, Timestamp timestamp) {
        if (statelessFilter(updateElement)) {
            MatchType match = statelessConvert(updateElement);
            if (direction == Direction.INSERT) {
                if (!matchLostEvents.remove(match)) // either had before but
                                                    // lost
                    matchFoundEvents.add(match); // or brand-new
            } else // revoke
            {
                if (!matchFoundEvents.remove(match)) // either never found
                                                     // in the first
                                                     // place
                    matchLostEvents.add(match); // or newly lost
            }
        }
    }

    @Override
    public void clear() {
        matchFoundEvents.clear();
        matchLostEvents.clear();
    }

}
