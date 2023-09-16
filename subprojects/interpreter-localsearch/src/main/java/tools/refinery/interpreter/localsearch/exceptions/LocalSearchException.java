/*******************************************************************************
 * Copyright (c) 2010-2013, Zoltan Ujhelyi, Akos Horvath, Istvan Rath and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.interpreter.localsearch.exceptions;

import tools.refinery.interpreter.matchers.InterpreterRuntimeException;

/**
 * @author Zoltan Ujhelyi, Akos Horvath
 *
 */
public class LocalSearchException extends InterpreterRuntimeException {

    private static final long serialVersionUID = -2585896573351435974L;

    public static final String PLAN_EXECUTION_ERROR = "Error while executing search plan";
    public static final String TYPE_ERROR = "Invalid type of variable";

    public LocalSearchException(String description, Throwable rootException) {
        super(description, rootException);
    }

    public LocalSearchException(String description) {
        super(description);
    }


}
