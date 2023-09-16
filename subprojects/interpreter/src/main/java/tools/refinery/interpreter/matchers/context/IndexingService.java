/*******************************************************************************
 * Copyright (c) 2010-2016, Grill Balázs, IncQueryLabs
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.interpreter.matchers.context;

import tools.refinery.interpreter.matchers.tuple.Tuple;

/**
 * These are the different services which can be provided by an {@link IQueryRuntimeContext} implementation.
 *
 * @author Grill Balázs
 * @since 1.4
 *
 */
public enum IndexingService {

    /**
     * Cardinality information is available. Makes possible to calculate
     * unseeded calls of {@link IQueryRuntimeContext#countTuples(IInputKey, Tuple)}
     */
    STATISTICS,

    /**
     * The indexer can provide notifications about changes in the model.
     */
    NOTIFICATIONS,

    /**
     * Enables enumeration of instances and reverse-navigation.
     */
    INSTANCES

}
