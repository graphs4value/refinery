/*******************************************************************************
 * Copyright (c) 2004-2010 Gabor Bergmann and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package tools.refinery.interpreter.matchers.psystem;

import java.util.Set;

import tools.refinery.interpreter.matchers.planning.SubPlan;
import tools.refinery.interpreter.matchers.context.IQueryMetaContext;

/**
 * Any constraint that can only be checked on certain SubPlans (e.g. those plans that already contain some variables).
 *
 * @author Gabor Bergmann
 *
 */
public abstract class DeferredPConstraint extends BasePConstraint {

    public DeferredPConstraint(PBody pBody, Set<PVariable> affectedVariables) {
        super(pBody, affectedVariables);
    }

    public abstract boolean isReadyAt(SubPlan plan, IQueryMetaContext context);

}
