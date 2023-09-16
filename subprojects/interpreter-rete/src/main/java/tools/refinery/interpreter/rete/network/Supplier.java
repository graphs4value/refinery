/*******************************************************************************
 * Copyright (c) 2004-2008 Gabor Bergmann and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package tools.refinery.interpreter.rete.network;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import tools.refinery.interpreter.rete.network.communication.Timestamp;
import tools.refinery.interpreter.rete.traceability.TraceInfo;
import tools.refinery.interpreter.matchers.tuple.Tuple;
import tools.refinery.interpreter.matchers.tuple.TupleMask;
import tools.refinery.interpreter.matchers.util.timeline.Timeline;
import tools.refinery.interpreter.rete.index.ProjectionIndexer;
import tools.refinery.interpreter.rete.single.TrimmerNode;

/**
 * @author Gabor Bergmann
 *
 *         A supplier is an object that can propagate insert or revoke events towards receivers.
 */
public interface Supplier extends Node {

    /**
     * Pulls the contents of this object in this particular moment into a target collection.
     *
     * @param flush if true, flushing of messages is allowed during the pull, otherwise flushing is not allowed
     * @since 2.3
     */
    public void pullInto(Collection<Tuple> collector, boolean flush);

    /**
     * @since 2.4
     */
    public void pullIntoWithTimeline(final Map<Tuple, Timeline<Timestamp>> collector, final boolean flush);

    /**
     * Returns the contents of this object in this particular moment.
     * For memoryless nodes, this may involve a costly recomputation of contents.
     *
     * The result is returned as a Set, even when it has multiplicities (at the output of {@link TrimmerNode}).
     *
     *  <p> Intended mainly for debug purposes; therefore flushing is performed only if explicitly requested
     *  During runtime, flushing may be preferred; see {@link ReteContainer#pullContents(Supplier)}
     * @since 2.3
     */
    public Set<Tuple> getPulledContents(boolean flush);

    default public Set<Tuple> getPulledContents() {
        return getPulledContents(true);
    }

    /**
     * appends a receiver that will continously receive insert and revoke updates from this supplier
     */
    void appendChild(Receiver receiver);

    /**
     * removes a receiver
     */
    void removeChild(Receiver receiver);

    /**
     * Instantiates (or reuses, depending on implementation) an index according to the given mask.
     *
     * Intended for internal use; clients should invoke through Library instead to enable reusing.
     */
    ProjectionIndexer constructIndex(TupleMask mask, TraceInfo... traces);

    /**
     * lists receivers
     */
    Collection<Receiver> getReceivers();

}
