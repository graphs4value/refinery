/*******************************************************************************
 * Copyright (c) 2004-2009 Gabor Bergmann and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package tools.refinery.interpreter.rete.index;

/**
 * An iterable indexer that receives updates from a node, and groups received tuples intact, i.e. it does not reduce
 * tuple groups.
 *
 * @author Gabor Bergmann
 *
 */
public interface ProjectionIndexer extends IterableIndexer {

}
