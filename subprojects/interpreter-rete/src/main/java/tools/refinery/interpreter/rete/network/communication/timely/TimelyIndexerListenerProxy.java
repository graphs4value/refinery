/*******************************************************************************
 * Copyright (c) 2010-2019, Tamas Szabo, Istvan Rath and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.interpreter.rete.network.communication.timely;

import tools.refinery.interpreter.matchers.tuple.Tuple;
import tools.refinery.interpreter.matchers.util.Direction;
import tools.refinery.interpreter.matchers.util.Preconditions;
import tools.refinery.interpreter.rete.index.IndexerListener;
import tools.refinery.interpreter.rete.network.Node;
import tools.refinery.interpreter.rete.network.ProductionNode;
import tools.refinery.interpreter.rete.network.communication.Timestamp;

/**
 * A timely proxy for another {@link IndexerListener}, which performs some preprocessing
 * on the differential timestamps before passing it on to the real recipient.
 * <p>
 * These proxies are used on edges leading into {@link ProductionNode}s. Because {@link ProductionNode}s
 * never ask back the indexer for its contents, there is no need to also apply the proxy on that direction.
 *
 * @author Tamas Szabo
 * @since 2.3
 */
public class TimelyIndexerListenerProxy implements IndexerListener {

    protected final TimestampTransformation preprocessor;
    protected final IndexerListener wrapped;

    public TimelyIndexerListenerProxy(final IndexerListener wrapped,
            final TimestampTransformation preprocessor) {
        Preconditions.checkArgument(!(wrapped instanceof TimelyIndexerListenerProxy), "Proxy in a proxy is not allowed!");
        this.wrapped = wrapped;
        this.preprocessor = preprocessor;
    }

    public IndexerListener getWrappedIndexerListener() {
        return wrapped;
    }

    @Override
    public Node getOwner() {
        return this.wrapped.getOwner();
    }

    @Override
    public void notifyIndexerUpdate(final Direction direction, final Tuple updateElement, final Tuple signature,
            final boolean change, final Timestamp timestamp) {
        this.wrapped.notifyIndexerUpdate(direction, updateElement, signature, change, preprocessor.process(timestamp));
    }

    @Override
    public String toString() {
        return this.preprocessor.toString() + "_PROXY -> " + this.wrapped.toString();
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        } else if (obj == this) {
            return true;
        } else {
            final TimelyIndexerListenerProxy that = (TimelyIndexerListenerProxy) obj;
            return this.wrapped.equals(that.wrapped) && this.preprocessor == that.preprocessor;
        }
    }

    @Override
    public int hashCode() {
        int hash = 1;
        hash = hash * 17 + this.wrapped.hashCode();
        hash = hash * 31 + this.preprocessor.hashCode();
        return hash;
    }

}
