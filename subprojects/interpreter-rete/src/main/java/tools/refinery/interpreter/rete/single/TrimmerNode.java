/*******************************************************************************
 * Copyright (c) 2004-2008 Gabor Bergmann and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package tools.refinery.interpreter.rete.single;

import tools.refinery.interpreter.rete.network.ReteContainer;
import tools.refinery.interpreter.matchers.tuple.Tuple;
import tools.refinery.interpreter.matchers.tuple.TupleMask;

/**
 * Trims the matchings as specified by a mask.
 *
 * @author Gabor Bergmann
 *
 */
public class TrimmerNode extends TransformerNode {

    protected TupleMask mask;

    /**
     * @param reteContainer
     * @param mask
     *            The mask used to trim substitutions.
     */
    public TrimmerNode(ReteContainer reteContainer, TupleMask mask) {
        super(reteContainer);
        this.mask = mask;
    }

    public TrimmerNode(ReteContainer reteContainer) {
        super(reteContainer);
        this.mask = null;
    }

    /**
     * @return the mask
     */
    public TupleMask getMask() {
        return mask;
    }

    /**
     * @param mask
     *            the mask to set
     */
    public void setMask(TupleMask mask) {
        this.mask = mask;
    }

    @Override
    protected Tuple transform(Tuple input) {
        return mask.transform(input);
    }

}
