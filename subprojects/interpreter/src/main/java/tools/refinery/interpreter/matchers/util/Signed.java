/*******************************************************************************
 * Copyright (c) 2010-2019, Tamas Szabo, itemis AG, Gabor Bergmann, IncQuery Labs Ltd.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.interpreter.matchers.util;

import java.util.Objects;

/**
 * A piece of data associated with a direction.
 *
 * @author Tamas Szabo
 * @since 2.4
 */
public class Signed<Payload extends Comparable<Payload>> {

    private final Payload payload;
    private final Direction direction;

    public Signed(final Direction direction, final Payload payload) {
        this.payload = payload;
        this.direction = direction;
    }

    public Payload getPayload() {
        return payload;
    }

    public Direction getDirection() {
        return direction;
    }

    @Override
    public int hashCode() {
        return Objects.hash(direction, payload);
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        } else if (obj == null || this.getClass() != obj.getClass()) {
            return false;
        } else {
            @SuppressWarnings("rawtypes")
            final Signed other = (Signed) obj;
            return direction == other.direction && Objects.equals(payload, other.payload);
        }
    }

    @Override
    public String toString() {
        return this.direction.asSign() + this.payload.toString();
    }

}
