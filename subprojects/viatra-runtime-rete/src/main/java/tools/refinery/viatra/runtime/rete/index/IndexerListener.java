/*******************************************************************************
 * Copyright (c) 2004-2008 Gabor Bergmann and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package tools.refinery.viatra.runtime.rete.index;

import tools.refinery.viatra.runtime.matchers.tuple.Tuple;
import tools.refinery.viatra.runtime.matchers.util.Direction;
import tools.refinery.viatra.runtime.rete.network.Node;
import tools.refinery.viatra.runtime.rete.network.communication.Timestamp;

/**
 * A listener for update events concerning an Indexer.
 * 
 * @author Gabor Bergmann
 * 
 */
public interface IndexerListener {
    /**
     * Notifies recipient that the indexer has just received an update. Contract: indexer already reflects the updated
     * state.
     * 
     * @param direction
     *            the direction of the update.
     * @param updateElement
     *            the tuple that was updated.
     * @param signature
     *            the signature of the tuple according to the indexer's mask.
     * @param change
     *            whether this was the first inserted / last revoked update element with this particular signature.
     * @since 2.4
     */
    void notifyIndexerUpdate(Direction direction, Tuple updateElement, Tuple signature, boolean change, Timestamp timestamp);

    Node getOwner();
}
