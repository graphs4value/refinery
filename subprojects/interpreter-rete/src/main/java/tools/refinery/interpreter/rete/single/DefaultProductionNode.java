/*******************************************************************************
 * Copyright (c) 2004-2008 Gabor Bergmann and Daniel Varro
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
import tools.refinery.interpreter.matchers.context.IPosetComparator;
import tools.refinery.interpreter.matchers.tuple.Tuple;
import tools.refinery.interpreter.matchers.tuple.TupleMask;
import tools.refinery.interpreter.rete.traceability.CompiledQuery;

/**
 * Default implementation of the Production node, based on UniquenessEnforcerNode
 *
 * @author Gabor Bergmann
 * @noinstantiate This class is not intended to be instantiated by clients.
 */
public class DefaultProductionNode extends UniquenessEnforcerNode implements ProductionNode {

    protected final Map<String, Integer> posMapping;

    /**
     * @since 1.6
     */
    public DefaultProductionNode(final ReteContainer reteContainer, final Map<String, Integer> posMapping,
                                 final boolean deleteRederiveEvaluation) {
        this(reteContainer, posMapping, deleteRederiveEvaluation, null, null, null);
    }

    /**
     * @since 1.6
     */
    public DefaultProductionNode(final ReteContainer reteContainer, final Map<String, Integer> posMapping,
            final boolean deleteRederiveEvaluation, final TupleMask coreMask, final TupleMask posetMask,
            final IPosetComparator posetComparator) {
        super(reteContainer, posMapping.size(), deleteRederiveEvaluation, coreMask, posetMask, posetComparator);
        this.posMapping = posMapping;
    }

    @Override
    public Map<String, Integer> getPosMapping() {
        return posMapping;
    }

    @Override
    public Iterator<Tuple> iterator() {
        return memory.iterator();
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
