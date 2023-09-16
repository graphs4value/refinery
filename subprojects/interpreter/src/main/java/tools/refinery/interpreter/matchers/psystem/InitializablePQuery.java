/*******************************************************************************
 * Copyright (c) 2010-2014, Zoltan Ujhelyi, Istvan Rath and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.interpreter.matchers.psystem;

import java.util.Set;

import tools.refinery.interpreter.matchers.psystem.annotations.PAnnotation;
import tools.refinery.interpreter.matchers.psystem.queries.PProblem;
import tools.refinery.interpreter.matchers.psystem.queries.PQuery;
import tools.refinery.interpreter.matchers.InterpreterRuntimeException;

/**
 * Adds extra methods to the PQuery interface to initialize its contents.
 *
 * @author Zoltan Ujhelyi
 *
 */
public interface InitializablePQuery extends PQuery {

    /**
     * Sets the query status. Only applicable if the pattern is still {@link PQueryStatus#UNINITIALIZED uninitialized}.
     *
     * @param status the new status
     */
    void setStatus(PQueryStatus status);

    /**
     * Adds a detected error. Only applicable if the pattern is still {@link PQueryStatus#UNINITIALIZED uninitialized}.
     *
     * @param problem the new problem
     */
    void addError(PProblem problem);

    /**
     * Sets up the bodies of the pattern. Only applicable if the pattern is still {@link PQueryStatus#UNINITIALIZED
     * uninitialized}.
     *
     * @param bodies
     * @throws InterpreterRuntimeException
     */
    void initializeBodies(Set<PBody> bodies);

    /**
     * Adds an annotation to the specification. Only applicable if the pattern is still
     * {@link PQueryStatus#UNINITIALIZED uninitialized}.
     *
     * @param annotation
     */
    void addAnnotation(PAnnotation annotation);
}
