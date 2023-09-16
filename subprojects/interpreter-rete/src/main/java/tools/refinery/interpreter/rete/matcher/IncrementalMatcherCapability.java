/*******************************************************************************
 * Copyright (c) 2010-2016, Grill Balázs, IncQuery Labs Ltd.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.interpreter.rete.matcher;

import tools.refinery.interpreter.matchers.backend.IMatcherCapability;

/**
 * @author Grill Balázs
 * @since 1.4
 *
 */
public class IncrementalMatcherCapability implements IMatcherCapability {

    @Override
    public boolean canBeSubstitute(IMatcherCapability capability) {
        /*
         * TODO: for now, as we are only prepared for Rete and LS, we can assume that
         * a matcher created with Rete can always be a substitute for a matcher created
         * by any backend.
         */
        return true;
    }

}
