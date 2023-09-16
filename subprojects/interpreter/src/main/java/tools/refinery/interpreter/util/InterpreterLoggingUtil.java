/*******************************************************************************
 * Copyright (c) 2010-2013, Bergmann Gabor, Istvan Rath and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.interpreter.util;

import org.apache.log4j.Logger;

/**
 * Centralized logger of the Refinery Interpreter runtime.
 * @author Bergmann Gabor
 *
 */
public class InterpreterLoggingUtil {

    private InterpreterLoggingUtil() {/*Utility class constructor*/}

    /*
    *
     * Provides a static default logger.
     */
    public static Logger getDefaultLogger() {
        if (defaultRuntimeLogger == null) {
			defaultRuntimeLogger = Logger.getLogger("tools.refinery.interpreter");
            if (defaultRuntimeLogger == null)
                throw new AssertionError(
						"Configuration error: unable to create default Refinery Interpreter runtime logger.");
        }

        return defaultRuntimeLogger;
    }

    private static String getLoggerClassname(Class<?> clazz) {
        return clazz.getName().startsWith(getDefaultLogger().getName())
                ? clazz.getName()
                : getDefaultLogger().getName() + "." + clazz.getName();
    }

    /**
     * Provides a class-specific logger that also stores the global logger settings of the Refinery Interpreter runtime
     * @param clazz
     */
    public static Logger getLogger(Class<?> clazz) {
        return Logger.getLogger(getLoggerClassname(clazz));
    }

    /**
     * Provides a named logger that also stores the global logger settings of the Refinery Interpreter runtime
     * @param clazz
     * @param name a non-empty name to append to the class names
     * @since 2.5
     */
    public static Logger getLogger(Class<?> clazz, String name) {
        return Logger.getLogger(getLoggerClassname(clazz) + '.' + name);
    }

    private static Logger defaultRuntimeLogger;
}
