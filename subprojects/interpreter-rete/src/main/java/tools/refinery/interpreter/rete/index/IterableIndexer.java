/*******************************************************************************
 * Copyright (c) 2004-2009 Gabor Bergmann and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package tools.refinery.interpreter.rete.index;

import tools.refinery.interpreter.matchers.tuple.Tuple;

/**
 * An indexer that allows the iteration of all retrievable tuple groups (or reduced groups).
 *
 * @author Gabor Bergmann
 *
 */
public interface IterableIndexer extends Indexer, Iterable<Tuple> {

    /**
     * A view consisting of exactly those signatures whose tuple group is not empty
     * @since 2.0
     */
    public Iterable<Tuple> getSignatures();

    /**
     * @return the number of signatures whose tuple group is not empty
     * @since 2.0
     */
    public int getBucketCount();

}
