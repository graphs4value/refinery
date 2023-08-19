/*******************************************************************************
 * Copyright (c) 2010-2013, Abel Hegedus, Istvan Rath and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.viatra.runtime.base.api;

import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EStructuralFeature;


/**
 * Listener interface for lightweight observation on EObject feature value changes.
 * 
 * @author Abel Hegedus
 *
 */
public interface LightweightEObjectObserver {
    
    /**
     * 
     * @param host
     * @param feature
     * @param notification
     */
    void notifyFeatureChanged(EObject host, EStructuralFeature feature, Notification notification);
    
}
