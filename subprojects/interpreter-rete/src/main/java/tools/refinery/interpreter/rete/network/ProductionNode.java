/*******************************************************************************
 * Copyright (c) 2004-2008 Gabor Bergmann and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package tools.refinery.interpreter.rete.network;

import java.util.Map;

import tools.refinery.interpreter.matchers.tuple.Tuple;

/**
 * Interface intended for nodes containing complete matches.
 *
 * @author Gabor Bergmann
 */
public interface ProductionNode extends Tunnel, Iterable<Tuple> {

    /**
     * @return the position mapping of this particular pattern that maps members of the tuple type to their positions
     */
    Map<String, Integer> getPosMapping();

}
