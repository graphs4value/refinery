/*******************************************************************************
 * Copyright (c) 2010-2015, Bergmann Gabor, Istvan Rath and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.interpreter.matchers.backend;

import tools.refinery.interpreter.matchers.psystem.queries.PQuery;

/**
 * Provides query evaluation hints consisting of the Engine default hints and
 * the hints provided by the pattern itself.
 *
 * @author Bergmann Gabor
 * @since 0.9
 * @noimplement This interface is not intended to be implemented by clients, except in the tools.refinery.viatra.runtime plugin.
 */
public interface IQueryBackendHintProvider {

    /**
     * Suggests query evaluation hints regarding a query. The returned hints reflects the default hints of the
     * query engine merged with the hints provided by the pattern itself. These can be overridden via specific
     * advanced API of the engine.
     *
     * @since 1.4
     */
    QueryEvaluationHint getQueryEvaluationHint(PQuery query);

}
