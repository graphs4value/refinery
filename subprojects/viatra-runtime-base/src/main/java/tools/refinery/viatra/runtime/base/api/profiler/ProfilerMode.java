/*******************************************************************************
 * Copyright (c) 2010-2019, Zoltan Ujhelyi, IncQuery Labs Ltd.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.viatra.runtime.base.api.profiler;

/**
 * @since 2.3
 */
public enum ProfilerMode {

    /** The base index profiler is not available */
    OFF,
    /** The profiler is initialized but not started until necessary */ 
    START_DISABLED,
    /** The profiler is initialized and started by default */
    START_ENABLED
}
