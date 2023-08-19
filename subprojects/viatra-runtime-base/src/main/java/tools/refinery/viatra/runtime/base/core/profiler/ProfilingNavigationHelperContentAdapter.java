/*******************************************************************************
 * Copyright (c) 2010-2019, Laszlo Gati, Zoltan Ujhelyi, IncQuery Labs Ltd.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.viatra.runtime.base.core.profiler;

import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.common.notify.Notifier;
import tools.refinery.viatra.runtime.base.core.NavigationHelperContentAdapter;
import tools.refinery.viatra.runtime.base.core.NavigationHelperImpl;

/**
 * 
 * @noinstantiate This class is not intended to be instantiated by clients.
 * @noreference This class is not intended to be referenced by clients.
 */
public final class ProfilingNavigationHelperContentAdapter extends NavigationHelperContentAdapter {

    private static class StopWatch {

        private long currentStartTimeNs = 0l;
        private long totalElapsedTimeNs = 0l;
        private boolean running = false;

        /**
         * Puts the timer in running state and saves the current time.
         */
        private void start() {
            currentStartTimeNs = System.nanoTime();
            running = true;

        }

        /**
         * Puts the the timer in stopped state and saves the total time spent in started
         * state between the last reset and now
         */
        private void stop() {
            totalElapsedTimeNs = getTotalElapsedTimeNs();
            running = false;
        }

        /**
         * @return time between the last start and now
         */
        private long getCurrentElapsedTimeNs() {
            return System.nanoTime() - currentStartTimeNs;
        }

        /**
         * @return the total time spent in started state between the last reset and now
         */
        private long getTotalElapsedTimeNs() {
            return running ? getCurrentElapsedTimeNs() + totalElapsedTimeNs : totalElapsedTimeNs;
        }
        
        /**
         * Saves the current time and resets all the time spent between the last reset and now.
         */
        private void resetTime() {
            currentStartTimeNs = System.currentTimeMillis();
            totalElapsedTimeNs = 0;
        }
    }

    long notificationCount = 0l;
    StopWatch watch = new StopWatch();
    boolean isEnabled = false;
    
    boolean measurement = false;

    public ProfilingNavigationHelperContentAdapter(NavigationHelperImpl navigationHelper, boolean enabled) {
        super(navigationHelper);
        this.isEnabled = enabled;
    }

    @Override
    public void notifyChanged(Notification notification) {
    	// Handle possibility of reentrancy
    	if (isEnabled && !measurement) {
    		try {
    			measurement = true;
    			notificationCount++;
    			watch.start();
    			super.notifyChanged(notification);
    		} finally {
    			watch.stop();
    			measurement = false;
    		}
    	} else {
    		super.notifyChanged(notification);
    	}
    }

    @Override
    public void setTarget(Notifier target) {
    	// Handle possibility of reentrancy
    	if (isEnabled && !measurement) {
    		try {
    			measurement = true;
    			notificationCount++;
    			watch.start();
    			super.setTarget(target);
    		} finally {
    			watch.stop();
    			measurement = false;
    		}
    	} else {
    		super.setTarget(target);
    	}
    }

    @Override
    public void unsetTarget(Notifier target) {
    	// Handle possibility of reentrancy
    	if (isEnabled && !measurement) {
    		try {
    			measurement = true;
    			notificationCount++;
    			watch.start();
    			super.unsetTarget(target);
    		} finally {
    			watch.stop();
    			measurement = false;
    		}
    	} else {
    		super.unsetTarget(target);
    	}
    }
    
    public long getNotificationCount() {
        return notificationCount;
    }

    public long getTotalMeasuredTimeInMS() {
        return watch.getTotalElapsedTimeNs() / 1_000_000l;
    }

    public boolean isEnabled() {
        return isEnabled;
    }

    public void setEnabled(boolean isEnabled) {
        this.isEnabled = isEnabled;
    }

    public void resetMeasurement() {
        notificationCount = 0;
        watch.resetTime();
    }
}