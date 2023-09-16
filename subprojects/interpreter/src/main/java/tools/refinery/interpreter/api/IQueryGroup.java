/*******************************************************************************
 * Copyright (c) 2010-2012, Mark Czotter, Istvan Rath and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.interpreter.api;

import java.util.Set;

/**
 * Generic interface for group of query specifications.
 *
 * <p>It handles more than one patterns as a group, and provides functionality to initialize the matchers together (which
 * has performance benefits).
 *
 * @author Mark Czotter
 *
 */
public interface IQueryGroup {

    /**
     * Initializes matchers for the group of patterns within an {@link InterpreterEngine}. If some of the pattern matchers are already
     * constructed in the engine, no task is performed for them.
     *
     * <p>
     * This preparation step has the advantage that it prepares pattern matchers for an arbitrary number of patterns in a
     * single-pass traversal of the model.
     * This is typically more efficient than traversing the model each time an individual pattern matcher is initialized on demand.
     * The performance benefit only manifests itself if the engine is not in wildcard mode.
     *
     * @param engine
     *            the existing Refinery Interpreter engine in which the matchers will be created.
     * @throws ViatraQueryRuntimeException
     *             if there was an error in preparing the engine
     */
    public void prepare(InterpreterEngine engine);

    /**
     * Returns the currently assigned {@link IQuerySpecification}s.
     */
    public Set<IQuerySpecification<?>> getSpecifications();

}
