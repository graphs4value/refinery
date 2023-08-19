/*******************************************************************************
 * Copyright (c) 2010-2019, Zoltan Ujhelyi, IncQuery Labs Ltd.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.viatra.runtime.base.api.profiler;

import tools.refinery.viatra.runtime.base.api.NavigationHelper;
import tools.refinery.viatra.runtime.base.core.NavigationHelperContentAdapter;
import tools.refinery.viatra.runtime.base.core.NavigationHelperImpl;
import tools.refinery.viatra.runtime.base.core.profiler.ProfilingNavigationHelperContentAdapter;
import tools.refinery.viatra.runtime.matchers.util.Preconditions;

/**
 * An index profiler can be attached to an existing navigation helper instance to access the profiling data and control
 * the profiler itself. If the NavigationHelper was not started in profiling mode, the profiler cannot be initialized.
 * 
 * @since 2.3
 */
public class BaseIndexProfiler {

    ProfilingNavigationHelperContentAdapter adapter;
    
    /**
     * 
     * @throws IllegalArgumentException if the profiler cannot be attached to the base index instance
     */
    public BaseIndexProfiler(NavigationHelper navigationHelper) {
        if (navigationHelper instanceof NavigationHelperImpl) {
            final NavigationHelperContentAdapter contentAdapter = ((NavigationHelperImpl) navigationHelper).getContentAdapter();
            if (contentAdapter instanceof ProfilingNavigationHelperContentAdapter) {
                adapter = (ProfilingNavigationHelperContentAdapter)contentAdapter;
            }
        }
        Preconditions.checkArgument(adapter != null, "Cannot attach profiler to Base Index");
    }

    /**
     * Returns the number of external request (e.g. model changes) the profiler recorded. 
     */
    public long getNotificationCount() {
        return adapter.getNotificationCount();
    }

    /**
     * Return the total time base index profiler recorded for reacting to model operations. 
     */
    public long getTotalMeasuredTimeInMS() {
        return adapter.getTotalMeasuredTimeInMS();
    }

    /**
     * Returns whether the profiler is turned on (e.g. measured values are increased).
     */
    public boolean isEnabled() {
        return adapter.isEnabled();
    }

    /**
     * Enables the base index profiling (e.g. measured values are increased)
     */
    public void setEnabled(boolean isEnabled) {
        adapter.setEnabled(isEnabled);
    }

    /**
     * Resets all measurements to 0, regardless whether the profiler is enabled or not.
     * </p>
     * 
     * <strong>Note</strong>: The behavior of the profiler is undefined when the measurements are reset while an EMF
     * notification is being processed and the profiler is enabled.
     */
    public void resetMeasurement() {
        adapter.resetMeasurement();
    }
}
