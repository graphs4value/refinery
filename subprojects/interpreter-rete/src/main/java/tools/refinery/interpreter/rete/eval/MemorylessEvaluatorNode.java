/*******************************************************************************
 * Copyright (c) 2010-2013, Bergmann Gabor, Istvan Rath and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.interpreter.rete.eval;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import tools.refinery.interpreter.rete.network.ReteContainer;
import tools.refinery.interpreter.rete.network.communication.Timestamp;
import tools.refinery.interpreter.matchers.tuple.Tuple;
import tools.refinery.interpreter.matchers.util.CollectionsFactory;
import tools.refinery.interpreter.matchers.util.Direction;
import tools.refinery.interpreter.matchers.util.timeline.Timeline;

/**
 * @author Bergmann Gabor
 *
 */
public class MemorylessEvaluatorNode extends AbstractEvaluatorNode {

    /**
     * @since 1.5
     */
    public MemorylessEvaluatorNode(final ReteContainer reteContainer, final EvaluatorCore core) {
        super(reteContainer, core);
    }

    @Override
    public void pullInto(final Collection<Tuple> collector, final boolean flush) {
        final Collection<Tuple> parentTuples = new ArrayList<Tuple>();
        propagatePullInto(parentTuples, flush);
        for (final Tuple parentTuple : parentTuples) {
            final Iterable<Tuple> output = core.performEvaluation(parentTuple);
            if (output != null) {
                final Iterator<Tuple> itr = output.iterator();
                while (itr.hasNext()) {
                    collector.add(itr.next());
                }
            }
        }
    }

    @Override
    public void pullIntoWithTimeline(final Map<Tuple, Timeline<Timestamp>> collector, final boolean flush) {
        final Map<Tuple, Timeline<Timestamp>> parentTuples = CollectionsFactory.createMap();
        propagatePullIntoWithTimestamp(parentTuples, flush);
        for (final Entry<Tuple, Timeline<Timestamp>> entry : parentTuples.entrySet()) {
            final Iterable<Tuple> output = core.performEvaluation(entry.getKey());
            if (output != null) {
                final Iterator<Tuple> itr = output.iterator();
                while (itr.hasNext()) {
                    collector.put(itr.next(), entry.getValue());
                }
            }
        }
    }

    @Override
    public void update(final Direction direction, final Tuple input, final Timestamp timestamp) {
        final Iterable<Tuple> output = core.performEvaluation(input);
        if (output != null) {
            propagateIterableUpdate(direction, output, timestamp);
        }
    }

}
