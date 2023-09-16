/*******************************************************************************
 * Copyright (c) 2010-2016, Tamas Szabo, Istvan Rath and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.interpreter.rete.network.communication.timely;

import tools.refinery.interpreter.rete.network.IGroupable;
import tools.refinery.interpreter.rete.network.Node;
import tools.refinery.interpreter.rete.network.communication.Timestamp;

/**
 * {@link Node}s that implement this interface can resume folding of their states when instructed during timely evaluation.
 *
 * @since 2.3
 * @author Tamas Szabo
 */
public interface ResumableNode extends Node, IGroupable {

    /**
     * When called, the folding of the state shall be resumed at the given timestamp. The resumable is expected to
     * do a folding step at the given timestamp only. Afterwards, folding shall be interrupted, even if there is more
     * folding to do towards higher timestamps.
     */
    public void resumeAt(final Timestamp timestamp);

    /**
     * Returns the smallest timestamp where lazy folding shall be resumed, or null if there is no more folding to do in this
     * resumable.
     */
    public Timestamp getResumableTimestamp();

}
