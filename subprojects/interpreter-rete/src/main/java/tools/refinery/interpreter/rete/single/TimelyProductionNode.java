/*******************************************************************************
 * Copyright (c) 2010-2019, Tamas Szabo, Istvan Rath and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.interpreter.rete.single;

import java.util.Iterator;
import java.util.Map;

import tools.refinery.interpreter.rete.network.ProductionNode;
import tools.refinery.interpreter.rete.network.ReteContainer;
import tools.refinery.interpreter.rete.traceability.TraceInfo;
import tools.refinery.interpreter.matchers.tuple.Tuple;
import tools.refinery.interpreter.rete.traceability.CompiledQuery;

/**
 * Differential dataflow implementation of the Production node, based on {@link TimelyUniquenessEnforcerNode}.
 *
 * @author Tamas Szabo
 * @noinstantiate This class is not intended to be instantiated by clients.
 * @since 2.3
 */
public class TimelyProductionNode extends TimelyUniquenessEnforcerNode implements ProductionNode {

    protected final Map<String, Integer> posMapping;

    public TimelyProductionNode(final ReteContainer reteContainer, final Map<String, Integer> posMapping) {
        super(reteContainer, posMapping.size());
        this.posMapping = posMapping;
    }

    @Override
    public Map<String, Integer> getPosMapping() {
        return this.posMapping;
    }

    @Override
    public Iterator<Tuple> iterator() {
        return this.memory.keySet().iterator();
    }

    @Override
    public void acceptPropagatedTraceInfo(final TraceInfo traceInfo) {
        if (traceInfo.propagateToProductionNodeParentAlso()) {
            super.acceptPropagatedTraceInfo(traceInfo);
        }
    }

    @Override
    public String toString() {
        for (final TraceInfo traceInfo : this.traceInfos) {
            if (traceInfo instanceof CompiledQuery) {
                final String patternName = ((CompiledQuery) traceInfo).getPatternName();
                return String.format(this.getClass().getName() + "<%s>=%s", patternName, super.toString());
            }
        }
        return super.toString();
    }

}
