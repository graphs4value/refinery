/*******************************************************************************
 * Copyright (c) 2010-2016, Grill Balázs, IncQuery Labs Ltd.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.interpreter.matchers.backend;

/**
 * Implementations of this interface can be used to decide whether a matcher created by an arbitrary backend can
 * potentially be used as a substitute for another matcher.
 *
 * @author Grill Balázs
 * @since 1.4
 *
 */
public interface IMatcherCapability {

    /**
     * Returns true if matchers of this capability can be used as a substitute for a matcher implementing the given capability
     */
    public boolean canBeSubstitute(IMatcherCapability capability);

}
