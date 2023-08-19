/*******************************************************************************
 * Copyright (c) 2010-2014, Bergmann Gabor, Istvan Rath and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.viatra.runtime.emf;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EStructuralFeature;
import tools.refinery.viatra.runtime.api.scope.IBaseIndex;
import tools.refinery.viatra.runtime.api.scope.IIndexingErrorListener;
import tools.refinery.viatra.runtime.api.scope.IInstanceObserver;
import tools.refinery.viatra.runtime.api.scope.ViatraBaseIndexChangeListener;
import tools.refinery.viatra.runtime.base.api.EMFBaseIndexChangeListener;
import tools.refinery.viatra.runtime.base.api.IEMFIndexingErrorListener;
import tools.refinery.viatra.runtime.base.api.LightweightEObjectObserver;
import tools.refinery.viatra.runtime.base.api.NavigationHelper;

/**
 * Wraps the EMF base index into the IBaseIndex interface.
 * @author Bergmann Gabor
 *
 */
public class EMFBaseIndexWrapper implements IBaseIndex {

    private final NavigationHelper navigationHelper;
    /**
     * @return the underlying index object
     */
    public NavigationHelper getNavigationHelper() {
        return navigationHelper;
    }

    /**
     * @param navigationHelper
     */
    public EMFBaseIndexWrapper(NavigationHelper navigationHelper) {
        this.navigationHelper = navigationHelper;
    }

    @Override
    public void resampleDerivedFeatures() {
        navigationHelper.resampleDerivedFeatures();
    }


    @Override
    public <V> V coalesceTraversals(Callable<V> callable) throws InvocationTargetException {
        return navigationHelper.coalesceTraversals(callable);
    }

    Map<IIndexingErrorListener, IEMFIndexingErrorListener> indexErrorListeners =
            new HashMap<IIndexingErrorListener, IEMFIndexingErrorListener>();
    @Override
    public boolean addIndexingErrorListener(final IIndexingErrorListener listener) {
        if (indexErrorListeners.containsKey(listener)) return false;
        IEMFIndexingErrorListener emfListener = new IEMFIndexingErrorListener() {
            @Override
            public void fatal(String description, Throwable t) {
                listener.fatal(description, t);
            }
            @Override
            public void error(String description, Throwable t) {
                listener.error(description, t);
            }
        };
        indexErrorListeners.put(listener, emfListener);
        return navigationHelper.addIndexingErrorListener(emfListener);
    }
    @Override
    public boolean removeIndexingErrorListener(IIndexingErrorListener listener) {
        if (!indexErrorListeners.containsKey(listener)) return false;
        return navigationHelper.removeIndexingErrorListener(indexErrorListeners.remove(listener));
    }
    
    
    Map<ViatraBaseIndexChangeListener, EMFBaseIndexChangeListener> indexChangeListeners = 
            new HashMap<ViatraBaseIndexChangeListener, EMFBaseIndexChangeListener>();
    @Override
    public void addBaseIndexChangeListener(final ViatraBaseIndexChangeListener listener) {
        EMFBaseIndexChangeListener emfListener = new EMFBaseIndexChangeListener() {
            @Override
            public boolean onlyOnIndexChange() {
                return listener.onlyOnIndexChange();
            }
            
            @Override
            public void notifyChanged(boolean indexChanged) {
                listener.notifyChanged(indexChanged);
            }
        };
        indexChangeListeners.put(listener, emfListener);
        navigationHelper.addBaseIndexChangeListener(emfListener);
    }
    @Override
    public void removeBaseIndexChangeListener(ViatraBaseIndexChangeListener listener) {
        final EMFBaseIndexChangeListener cListener = indexChangeListeners.remove(listener);
        if (cListener != null) 
            navigationHelper.removeBaseIndexChangeListener(cListener);
    }

    Map<IInstanceObserver, EObjectObserver> instanceObservers = 
                new HashMap<IInstanceObserver, EObjectObserver>();
    @Override
    public boolean addInstanceObserver(final IInstanceObserver observer,
            Object observedObject) {
        if (observedObject instanceof EObject) {
            EObjectObserver emfObserver = instanceObservers.computeIfAbsent(observer, EObjectObserver::new);
            boolean success = 
                    navigationHelper.addLightweightEObjectObserver(emfObserver, (EObject) observedObject);
            if (success) emfObserver.usageCount++;
            return success;
        } else return false;
    }
    @Override
    public boolean removeInstanceObserver(IInstanceObserver observer,
            Object observedObject) {
        if (observedObject instanceof EObject) {
            EObjectObserver emfObserver = instanceObservers.get(observer);
            if (emfObserver == null)
                return false;
            boolean success = 
                    navigationHelper.removeLightweightEObjectObserver(emfObserver, (EObject)observedObject);
            if (success) 
                if (0 == --emfObserver.usageCount)
                    instanceObservers.remove(observer);
            return success;
        } else return false;
    }
    private static class EObjectObserver implements LightweightEObjectObserver {
        /**
         * 
         */
        private final IInstanceObserver observer;
        int usageCount = 0; 

        /**
         * @param observer
         */
        private EObjectObserver(IInstanceObserver observer) {
            this.observer = observer;
        }

        @Override
        public void notifyFeatureChanged(EObject host,
                EStructuralFeature feature, Notification notification) {
            observer.notifyBinaryChanged(host, feature);
        }
    }

}