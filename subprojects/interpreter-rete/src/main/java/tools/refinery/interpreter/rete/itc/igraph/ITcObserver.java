/*******************************************************************************
 * Copyright (c) 2010-2012, Tamas Szabo, Istvan Rath and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package tools.refinery.interpreter.rete.itc.igraph;

/**
 * Interface ITcObserver is used to observe the changes in a transitive closure relation; tuple insertion/deletion.
 *
 * @author Szabo Tamas
 *
 */
public interface ITcObserver<V> {

    /**
     * Used to notify when a tuple is inserted into the transitive closure relation.
     *
     * @param source
     *            the source of the tuple
     * @param target
     *            the target of the tuple
     */
    public void tupleInserted(V source, V target);

    /**
     * Used to notify when a tuple is deleted from the transitive closure relation.
     *
     * @param source
     *            the source of the tuple
     * @param target
     *            the target of the tuple
     */
    public void tupleDeleted(V source, V target);
}
