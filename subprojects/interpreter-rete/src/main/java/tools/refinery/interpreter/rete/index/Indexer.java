/*******************************************************************************
 * Copyright (c) 2004-2008 Gabor Bergmann and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package tools.refinery.interpreter.rete.index;

import java.util.Collection;
import java.util.Map;

import tools.refinery.interpreter.rete.network.Node;
import tools.refinery.interpreter.rete.network.Supplier;
import tools.refinery.interpreter.rete.network.communication.Timestamp;
import tools.refinery.interpreter.matchers.tuple.Tuple;
import tools.refinery.interpreter.matchers.tuple.TupleMask;
import tools.refinery.interpreter.matchers.util.timeline.Timeline;

/**
 * A node that indexes incoming Tuples by their signatures as specified by a TupleMask. Notifies listeners about such
 * update events through the IndexerListener.
 *
 * Signature tuples are created by transforming the update tuples using the mask. Tuples stored with the same signature
 * are grouped together. The group or a reduction thereof is retrievable.
 *
 * @author Gabor Bergmann
 */
public interface Indexer extends Node {
    /**
     * @return the mask by which the contents are indexed.
     */
    public TupleMask getMask();

    /**
     * @return the node whose contents are indexed.
     */
    public Supplier getParent();

    /**
     * @return all stored tuples that conform to the specified signature, null if there are none such. CONTRACT: do not
     *         modify!
     */
    public Collection<Tuple> get(Tuple signature);

    /**
     * @since 2.4
     */
    default public Map<Tuple, Timeline<Timestamp>> getTimeline(Tuple signature) {
        throw new UnsupportedOperationException();
    }

    /**
     * This indexer will be updated whenever a Rete update is sent to the active node (or an equivalent time slot
     * allotted to it). The active node is typically the indexer itself, but it can be a different node such as its
     * parent.
     *
     * @return the active node that operates this indexer
     */
    public Node getActiveNode();


    public Collection<IndexerListener> getListeners();

    public void attachListener(IndexerListener listener);

    public void detachListener(IndexerListener listener);

}
