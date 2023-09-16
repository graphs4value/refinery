/*******************************************************************************
 * Copyright (c) 2004-2010 Akos Horvath and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.interpreter.exception;

import tools.refinery.interpreter.matchers.InterpreterRuntimeException;
import tools.refinery.interpreter.matchers.planning.QueryProcessingException;
import tools.refinery.interpreter.matchers.psystem.queries.QueryInitializationException;

import java.io.Serial;

/**
 * A general Refinery Interpreter-related problem during the operation of the Refinery Interpreter
 * engine, or the loading, manipulation and evaluation of queries.
 *
 * @author Bergmann Gabor
 * @since 0.9
 *
 */
public class InterpreterException extends InterpreterRuntimeException {

    @Serial
	private static final long serialVersionUID = -74252748358355750L;

    /**
     * @since 0.9
     */
    public static final String PROCESSING_PROBLEM = "The following error occurred during the processing of a query " +
			"(e.g. for the preparation of a Refinery pattern matcher)";
    /**
     * @since 0.9
     */
    public static final String QUERY_INIT_PROBLEM = "The following error occurred during the initialization of a " +
			"Refinery query specification";

    private final String shortMessage;

    public InterpreterException(String s, String shortMessage) {
        super(s);
        this.shortMessage = shortMessage;
    }

    public InterpreterException(QueryProcessingException e) {
        super(PROCESSING_PROBLEM + ": " + e.getMessage(), e);
        this.shortMessage = e.getShortMessage();
    }

    public InterpreterException(QueryInitializationException e) {
        super(QUERY_INIT_PROBLEM + ": " + e.getMessage(), e);
        this.shortMessage = e.getShortMessage();
    }

    public InterpreterException(String s, String shortMessage, Throwable e) {
        super(s + ": " + e.getMessage(), e);
        this.shortMessage = shortMessage;
    }

    public String getShortMessage() {
        return shortMessage;
    }

}
