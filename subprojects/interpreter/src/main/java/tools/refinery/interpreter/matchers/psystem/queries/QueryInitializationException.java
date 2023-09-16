/*******************************************************************************
 * Copyright (c) 2010-2015, Bergmann Gabor, Istvan Rath and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.interpreter.matchers.psystem.queries;

import tools.refinery.interpreter.matchers.planning.QueryProcessingException;

/**
 * Represent an exception that occurred while initializing the specification of a query.
 * @author Bergmann Gabor
 * @since 0.9
 *
 */
public class QueryInitializationException extends QueryProcessingException {

    public QueryInitializationException(String message, String[] context, String shortMessage, Object patternDescription,
            Throwable cause) {
        super(message, context, shortMessage, patternDescription, cause);
    }

    public QueryInitializationException(String message, String[] context, String shortMessage, Object patternDescription) {
        super(message, context, shortMessage, patternDescription);
    }

    private static final long serialVersionUID = 9106033062252951489L;




}
