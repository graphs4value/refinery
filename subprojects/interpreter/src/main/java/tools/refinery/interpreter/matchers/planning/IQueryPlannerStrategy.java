/*******************************************************************************
 * Copyright (c) 2004-2010 Gabor Bergmann and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package tools.refinery.interpreter.matchers.planning;

import org.apache.log4j.Logger;
import tools.refinery.interpreter.matchers.InterpreterRuntimeException;
import tools.refinery.interpreter.matchers.context.IQueryMetaContext;
import tools.refinery.interpreter.matchers.psystem.PBody;

/**
 * An algorithm that builds a query plan based on a PSystem representation of a body of constraints. This interface is
 * for internal use of the various query backends.
 *
 * @author Gabor Bergmann
 */
public interface IQueryPlannerStrategy {

    /**
     * @throws InterpreterRuntimeException
     */
    public SubPlan plan(PBody pSystem, Logger logger, IQueryMetaContext context);
}
