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
import tools.refinery.interpreter.rete.network.Receiver;
import tools.refinery.interpreter.rete.network.communication.CommunicationGroup;
import tools.refinery.interpreter.rete.network.communication.MessageSelector;
import tools.refinery.interpreter.rete.network.communication.Timestamp;
import tools.refinery.interpreter.rete.network.mailbox.Mailbox;

/**
 * A timely proxy for another {@link Mailbox}, which performs some preprocessing
 * on the differential timestamps before passing it on to the real recipient.
 *
 * @author Tamas Szabo
 * @since 2.3
 */
public class TimelyMailboxProxy implements Mailbox {

    protected final TimestampTransformation preprocessor;
    protected final Mailbox wrapped;

    public TimelyMailboxProxy(final Mailbox wrapped, final TimestampTransformation preprocessor) {
        Preconditions.checkArgument(!(wrapped instanceof TimelyMailboxProxy), "Proxy in a proxy is not allowed!");
        this.wrapped = wrapped;
        this.preprocessor = preprocessor;
    }

    public Mailbox getWrappedMailbox() {
        return wrapped;
    }

    @Override
    public void postMessage(final Direction direction, final Tuple update, final Timestamp timestamp) {
        this.wrapped.postMessage(direction, update, preprocessor.process(timestamp));
    }

    @Override
    public String toString() {
        return this.preprocessor.toString() + "_PROXY -> " + this.wrapped.toString();
    }

    @Override
    public void clear() {
        this.wrapped.clear();
    }

    @Override
    public void deliverAll(final MessageSelector selector) {
        this.wrapped.deliverAll(selector);
    }

    @Override
    public CommunicationGroup getCurrentGroup() {
        return this.wrapped.getCurrentGroup();
    }

    @Override
    public void setCurrentGroup(final CommunicationGroup group) {
        this.wrapped.setCurrentGroup(group);
    }

    @Override
    public Receiver getReceiver() {
        return this.wrapped.getReceiver();
    }

    @Override
    public boolean isEmpty() {
        return this.wrapped.isEmpty();
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        } else if (obj == this) {
            return true;
        } else {
            final TimelyMailboxProxy that = (TimelyMailboxProxy) obj;
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
