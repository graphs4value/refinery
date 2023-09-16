/*******************************************************************************
 * Copyright (c) 2010-2013, Zoltan Ujhelyi, Istvan Rath and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.interpreter.matchers.planning;

import tools.refinery.interpreter.matchers.InterpreterRuntimeException;

/**
 * @author Zoltan Ujhelyi
 * @since 0.9
 */
public class QueryProcessingException extends InterpreterRuntimeException {

    private static final long serialVersionUID = -8272290113656867086L;
    /**
     * Binding the '{n}' (n = 1..N) strings to contextual conditions in 'context'
     *
     * @param context
     *            : array of context-sensitive Strings
     */
    protected static String bind(String message, String[] context) {
        if (context == null)
            return message;

        String internal = message;
        for (int i = 0; i < context.length; i++) {
            internal = internal.replace("{" + (i + 1) + "}", context[i] != null ? context[i] : "<<null>>");
        }
        return internal;
    }

    private Object patternDescription;
    private String shortMessage;

    /**
     * @param message
     *            The template of the exception message
     * @param context
     *            The data elements to be used to instantiate the template. Can be null if no context parameter is
     *            defined
     * @param patternDescription
     *            the PatternDescription where the exception occurred
     * @since 2.0
     */
    public QueryProcessingException(String message, Object patternDescription) {
        super(message);
        initializeFields(message, patternDescription);
    }

    /**
     * @param message
     *            The template of the exception message
     * @param context
     *            The data elements to be used to instantiate the template. Can be null if no context parameter is
     *            defined
     * @param patternDescription
     *            the PatternDescription where the exception occurred
     */
    public QueryProcessingException(String message, String[] context, String shortMessage, Object patternDescription) {
        super(bind(message, context));
        initializeFields(shortMessage, patternDescription);
    }

    /**
     * @param message
     *            The template of the exception message
     * @param context
     *            The data elements to be used to instantiate the template. Can be null if no context parameter is
     *            defined
     * @param patternDescription
     *            the PatternDescription where the exception occurred
     */
    public QueryProcessingException(String message, String[] context, String shortMessage, Object patternDescription,
            Throwable cause) {
        super(bind(message, context), cause);
        initializeFields(shortMessage, patternDescription);
    }

    public Object getPatternDescription() {
        return patternDescription;
    }

    public String getShortMessage() {
        return shortMessage;
    }

    private void initializeFields(String shortMessage, Object patternDescription) {
        this.patternDescription = patternDescription;
        this.shortMessage = shortMessage;
    }


    public void setPatternDescription(Object patternDescription) {
        this.patternDescription = patternDescription;
    }

}
