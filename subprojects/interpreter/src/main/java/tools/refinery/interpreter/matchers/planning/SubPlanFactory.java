/*******************************************************************************
 * Copyright (c) 2010-2014, Bergmann Gabor, Istvan Rath and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.interpreter.matchers.planning;

import tools.refinery.interpreter.matchers.planning.operations.POperation;
import tools.refinery.interpreter.matchers.psystem.PBody;

/**
 * Single entry point for creating subplans.
 * Can be subclassed by query planner to provide specialized SubPlans.
 * @author Bergmann Gabor
 *
 */
public class SubPlanFactory {

    protected PBody body;

    public SubPlanFactory(PBody body) {
        super();
        this.body = body;
    }

    public SubPlan createSubPlan(POperation operation, SubPlan... parentPlans) {
        return new SubPlan(body, operation, parentPlans);
    }

}
