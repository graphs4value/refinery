/*******************************************************************************
 * Copyright (c) 2010-2016, Tamas Szabo, Istvan Rath and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.interpreter.rete.network;

import tools.refinery.interpreter.matchers.context.IPosetComparator;
import tools.refinery.interpreter.matchers.tuple.Tuple;
import tools.refinery.interpreter.matchers.tuple.TupleMask;
import tools.refinery.interpreter.matchers.util.Direction;

/**
 * @author Tamas Szabo
 * @since 2.0
 */
public interface PosetAwareReceiver extends Receiver {

    public TupleMask getCoreMask();

    public TupleMask getPosetMask();

    public IPosetComparator getPosetComparator();

    /**
     * Updates the receiver with a newly found or lost partial matching also providing information
     * whether the update is a monotone change or not.
     *
     * @param direction the direction of the update
     * @param update the update tuple
     * @param monotone true if the update is monotone, false otherwise
     * @since 2.4
     */
    public void updateWithPosetInfo(Direction direction, Tuple update, boolean monotone);

}
