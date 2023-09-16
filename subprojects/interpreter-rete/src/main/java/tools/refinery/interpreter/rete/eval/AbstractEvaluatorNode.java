/*******************************************************************************
 * Copyright (c) 2010-2013, Bergmann Gabor, Istvan Rath and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.interpreter.rete.eval;

import java.util.Iterator;

import tools.refinery.interpreter.rete.network.ReteContainer;
import tools.refinery.interpreter.rete.network.communication.Timestamp;
import tools.refinery.interpreter.rete.single.SingleInputNode;
import tools.refinery.interpreter.matchers.tuple.Tuple;
import tools.refinery.interpreter.matchers.util.Direction;

/**
 * @author Bergmann Gabor
 */
public abstract class AbstractEvaluatorNode extends SingleInputNode implements IEvaluatorNode {

    /**
     * @since 1.5
     */
    protected EvaluatorCore core;


    /**
     * @since 1.5
     */
    public AbstractEvaluatorNode(ReteContainer reteContainer, EvaluatorCore core) {
        super(reteContainer);
        this.core = core;
        core.init(this);
    }

    /**
     * @since 1.5
     */
    @Override
    public ReteContainer getReteContainer() {
        return getContainer();
    }

    /**
     * @since 1.5
     */
    @Override
    public String prettyPrintTraceInfoPatternList() {
        return getTraceInfoPatternsEnumerated();
    }

    /**
     * @since 2.4
     */
    protected void propagateIterableUpdate(final Direction direction, final Iterable<Tuple> update, final Timestamp timestamp) {
        final Iterator<Tuple> itr = update.iterator();
        while (itr.hasNext()) {
            propagateUpdate(direction, itr.next(), timestamp);
        }
    }

}
