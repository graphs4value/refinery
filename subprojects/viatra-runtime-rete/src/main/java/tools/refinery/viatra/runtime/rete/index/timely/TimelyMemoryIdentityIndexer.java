/*******************************************************************************
 * Copyright (c) 2010-2019, Tamas Szabo, Istvan Rath and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.viatra.runtime.rete.index.timely;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import tools.refinery.viatra.runtime.matchers.tuple.Tuple;
import tools.refinery.viatra.runtime.matchers.util.TimelyMemory;
import tools.refinery.viatra.runtime.matchers.util.timeline.Timeline;
import tools.refinery.viatra.runtime.rete.index.IdentityIndexer;
import tools.refinery.viatra.runtime.rete.network.Receiver;
import tools.refinery.viatra.runtime.rete.network.ReteContainer;
import tools.refinery.viatra.runtime.rete.network.Supplier;
import tools.refinery.viatra.runtime.rete.network.communication.Timestamp;

public class TimelyMemoryIdentityIndexer extends IdentityIndexer {

    protected final TimelyMemory<Timestamp> memory;

    public TimelyMemoryIdentityIndexer(final ReteContainer reteContainer, final int tupleWidth,
            final TimelyMemory<Timestamp> memory, final Supplier parent, final Receiver activeNode,
            final List<ListenerSubscription> sharedSubscriptionList) {
        super(reteContainer, tupleWidth, parent, activeNode, sharedSubscriptionList);
        this.memory = memory;
    }

    @Override
    public Map<Tuple, Timeline<Timestamp>> getTimeline(final Tuple signature) {
        final Timeline<Timestamp> timestamp = this.memory.get(signature);
        if (timestamp != null) {
            return Collections.singletonMap(signature, timestamp);
        } else {
            return null;
        }
    }
    
    @Override
    protected Collection<Tuple> getTuples() {
        return this.memory.getTuplesAtInfinity();
    }

}
