/*******************************************************************************
 * Copyright (c) 2010-2019, Tamas Szabo, Istvan Rath and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.interpreter.rete.network.communication.timely;

import tools.refinery.interpreter.rete.network.Node;
import tools.refinery.interpreter.rete.network.communication.Timestamp;

/**
 * Values of this enum perform different kind of preprocessing on {@link Timestamp}s.
 * This is used on edges leading in and out from {@link Node}s in recursive {@link TimelyCommunicationGroup}s.
 *
 * @author Tamas Szabo
 * @since 2.3
 */
public enum TimestampTransformation {

    INCREMENT {
        @Override
        public Timestamp process(final Timestamp timestamp) {
            return new Timestamp(timestamp.getValue() + 1);
        }

        @Override
        public String toString() {
            return "INCREMENT";
        }
    },
    RESET {
        @Override
        public Timestamp process(final Timestamp timestamp) {
            return Timestamp.ZERO;
        }

        @Override
        public String toString() {
            return "RESET";
        }
    };

    public abstract Timestamp process(final Timestamp timestamp);

}
