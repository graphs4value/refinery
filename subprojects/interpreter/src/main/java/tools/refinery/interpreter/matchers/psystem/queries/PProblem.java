/*******************************************************************************
 * Copyright (c) 2010-2014, Bergmann Gabor, Istvan Rath and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.interpreter.matchers.psystem.queries;

import tools.refinery.interpreter.matchers.planning.QueryProcessingException;

/**
 * Represents an error that was detected while the {@link PQuery} object was built from a source.
 * @author Bergmann Gabor
 *
 */
public class PProblem {

    private final String shortMessage;
    private final String location;
    private final Exception exception;

    public PProblem(String shortMessage) {
        this(null, shortMessage, null, null);
    }
    /**
     * @since 2.0
     */
    public PProblem(String shortMessage, Integer line, Integer column) {
        this(null, shortMessage, line, column);
    }
    public PProblem(QueryProcessingException exception) {
        this(exception, exception.getShortMessage(), null, null);
    }
    public PProblem(Exception exception, String shortMessage) {
        this(exception, shortMessage, null, null);
    }

    /**
     * @since 2.0
     */
    public PProblem(Exception exception, String shortMessage, Integer line, Integer column) {
        this.shortMessage = shortMessage;
        this.exception = exception;
        if (line == null) {
            location = "Unspecified location";
        } else if (column == null) {
            location = String.format("Line %d", line);
        } else {
            location = String.format("Line %d Column %d", line, column);
        }
    }

    public String getShortMessage() {
        return shortMessage;
    }
    public Exception getException() {
        return exception;
    }
    /**
     * @since 2.0
     */
    public String getLocation() {
        return location;
    }

}
