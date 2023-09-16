/*******************************************************************************
 * Copyright (c) 2010-2015, Bergmann Gabor, Istvan Rath and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.interpreter.rete.boundary;

import tools.refinery.interpreter.matchers.context.IInputKey;
import tools.refinery.interpreter.matchers.context.IQueryRuntimeContext;
import tools.refinery.interpreter.matchers.tuple.Tuple;
import tools.refinery.interpreter.matchers.tuple.TupleMask;
import tools.refinery.interpreter.rete.matcher.ReteEngine;
import tools.refinery.interpreter.rete.network.ReteContainer;
import tools.refinery.interpreter.rete.single.FilterNode;

/**
 * A filter node representing a (stateless, typically non-enumerable) extensional input relation.
 *
 * <p> Contains those tuples of its parents, that (when transformed by a mask, if given) are present in the extensional relation identified by the input key.
 *
 * @author Bergmann Gabor
 *
 */
public class ExternalInputStatelessFilterNode extends FilterNode implements Disconnectable {

    IQueryRuntimeContext context = null;
    IInputKey inputKey;
    private InputConnector inputConnector;
    private TupleMask mask;

    public ExternalInputStatelessFilterNode(ReteContainer reteContainer, TupleMask mask) {
        super(reteContainer);
        this.mask = mask;
        this.inputConnector = reteContainer.getNetwork().getInputConnector();
    }

    @Override
    public boolean check(Tuple ps) {
        if (mask != null)
            ps = mask.transform(ps);
        return context.containsTuple(inputKey, ps);
    }


    public void connectThroughContext(ReteEngine engine, IInputKey inputKey) {
        this.inputKey = inputKey;
        setTag(inputKey);

        final IQueryRuntimeContext context = engine.getRuntimeContext();
        if (!context.getMetaContext().isStateless(inputKey))
            throw new IllegalArgumentException(
                    this.getClass().getSimpleName() +
                    " only applicable for stateless input keys; received instead " +
                    inputKey);

        this.context = context;

        engine.addDisconnectable(this);
    }

    @Override
    public void disconnect() {
        this.context = null;
    }
}
