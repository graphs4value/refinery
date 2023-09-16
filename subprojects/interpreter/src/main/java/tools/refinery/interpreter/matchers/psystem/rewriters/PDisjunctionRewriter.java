/*******************************************************************************
 * Copyright (c) 2010-2014, Zoltan Ujhelyi, Istvan Rath and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.interpreter.matchers.psystem.rewriters;

import tools.refinery.interpreter.matchers.psystem.queries.PDisjunction;
import tools.refinery.interpreter.matchers.psystem.queries.PQuery;

/**
 * An abstract base class for creating alternative representations for PDisjunctions.
 * @author Zoltan Ujhelyi
 *
 */
public abstract class PDisjunctionRewriter extends AbstractRewriterTraceSource{

    public abstract PDisjunction rewrite(PDisjunction disjunction);

    public PDisjunction rewrite(PQuery query) {
        return rewrite(query.getDisjunctBodies());
    }

}
