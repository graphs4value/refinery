/*******************************************************************************
 * Copyright (c) 2004-2008 Gabor Bergmann and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package tools.refinery.interpreter.matchers.util;

/**
 * @author Gabor Bergmann
 * @since 1.7
 *         An instance of clearable pattern memory.
 */
public interface Clearable {
    /**
     * Clear all partial matchings stored in memory
     *
     */
    void clear();
}
