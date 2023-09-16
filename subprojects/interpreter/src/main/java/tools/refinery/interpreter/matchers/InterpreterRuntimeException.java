/*******************************************************************************
 * Copyright (c) 2010-2017, Zoltan Ujhelyi, IncQuery Labs Ltd.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.interpreter.matchers;

import java.io.Serial;

/**
 * A common base class for all exceptions thrown by various Refinery Interpreter APIs.
 *
 * @author Zoltan Ujhelyi
 * @since 2.0
 */
public abstract class InterpreterRuntimeException extends RuntimeException {

    @Serial
	private static final long serialVersionUID = -8505253058035069310L;

    public InterpreterRuntimeException() {
        super();
    }

    public InterpreterRuntimeException(String message) {
        super(message);
    }

    public InterpreterRuntimeException(Throwable cause) {
        super(cause);
    }

    public InterpreterRuntimeException(String message, Throwable cause) {
        super(message, cause);
    }

    public InterpreterRuntimeException(String message, Throwable cause, boolean enableSuppression,
									   boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

}
