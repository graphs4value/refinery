/*******************************************************************************
 * Copyright (c) 2010-2016, Tamas Szabo, Istvan Rath and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.interpreter.rete.network;

/**
 * A rederivable node can potentially re-derive tuples after the Rete network has finished the delivery of messages.
 *
 * @author Tamas Szabo
 * @since 1.6
 */
public interface RederivableNode extends Node, IGroupable {

    /**
     * The method is called by the {@link ReteContainer} to re-derive tuples after the normal messages have been
     * delivered and consumed. The re-derivation process may trigger the creation and delivery of further messages
     * and further re-derivation rounds.
     */
    public void rederiveOne();

    /**
     * Returns true if this node actually runs in DRed mode (not necessarily).
     *
     * @return true if the node is operating in DRed mode
     * @since 2.0
     */
    public boolean isInDRedMode();

}
