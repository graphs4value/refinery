/*******************************************************************************
 * Copyright (c) 2004-2009 Gabor Bergmann and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package tools.refinery.interpreter.rete.construction;

import tools.refinery.interpreter.matchers.planning.QueryProcessingException;

/**
 * A problem has occurred during the construction of the RETE net.
 *
 * @author Gabor Bergmann
 *
 */
public class RetePatternBuildException extends QueryProcessingException {

    private static final long serialVersionUID = 6966585498204577548L;

    /**
     * @param message
     *            The template of the exception message
     * @param context
     *            The data elements to be used to instantiate the template. Can be null if no context parameter is
     *            defined
     * @param patternDescription
     *            the PatternDescription where the exception occurred
     */
    public RetePatternBuildException(String message, String[] context, String shortMessage, Object patternDescription) {
        super(message, context, shortMessage, patternDescription);
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
    public RetePatternBuildException(String message, String[] context, String shortMessage, Object patternDescription,
            Throwable cause) {
        super(message, context, shortMessage, patternDescription, cause);
    }
}
