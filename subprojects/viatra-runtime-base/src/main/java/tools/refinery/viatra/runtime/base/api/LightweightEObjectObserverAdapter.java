/*******************************************************************************
 * Copyright (c) 2010-2013, Abel Hegedus, Istvan Rath and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.viatra.runtime.base.api;

import static tools.refinery.viatra.runtime.matchers.util.Preconditions.checkArgument;

import java.util.Collection;
import java.util.HashSet;

import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EStructuralFeature;

/**
 * Adapter class for lightweight observer which filters feature updates to a selected set of features.
 * 
 * @author Abel Hegedus
 *
 */
public abstract class LightweightEObjectObserverAdapter implements LightweightEObjectObserver {

    private Collection<EStructuralFeature> observedFeatures;
    
    /**
     * Creates a new adapter with the given set of observed features.
     */
    public LightweightEObjectObserverAdapter(Collection<EStructuralFeature> observedFeatures) {
        checkArgument(observedFeatures != null, "List of observed features must not be null!");
        this.observedFeatures = new HashSet<>(observedFeatures);
    }
    
    public void observeAdditionalFeature(EStructuralFeature observedFeature) {
        checkArgument(observedFeature != null, "Cannot observe null feature!");
        this.observedFeatures.add(observedFeature);
    }
    
    public void observeAdditionalFeatures(Collection<EStructuralFeature> observedFeatures) {
        checkArgument(observedFeatures != null, "List of additional observed features must not be null!");
        this.observedFeatures.addAll(observedFeatures);
    }
    
    public void removeObservedFeature(EStructuralFeature observedFeature) {
        checkArgument(observedFeature != null, "Cannot remove null observed feature!");
        this.observedFeatures.remove(observedFeature);
    }
    
    public void removeObservedFeatures(Collection<EStructuralFeature> observedFeatures) {
        checkArgument(observedFeatures != null, "List of observed features to remove must not be null!");
        this.observedFeatures.removeAll(observedFeatures);
    }
    
    @Override
    public void notifyFeatureChanged(EObject host, EStructuralFeature feature, Notification notification) {
        if(this.observedFeatures.contains(feature)) {
            observedFeatureUpdate(host, feature, notification);
        }
    }
    
    /**
     * This method is called when the feature that changed is among the observed features of the adapter.
     * 
     * @param host
     * @param feature
     * @param notification
     */
    public abstract void observedFeatureUpdate(EObject host, EStructuralFeature feature, Notification notification);

}
