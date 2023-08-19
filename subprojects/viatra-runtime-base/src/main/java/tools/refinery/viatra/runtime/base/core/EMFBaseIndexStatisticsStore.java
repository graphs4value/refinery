/*******************************************************************************
 * Copyright (c) 2010-2016, Grill Balázs, IncQuery Labs Ltd.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.viatra.runtime.base.core;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EStructuralFeature;

/**
 * @author Grill Balázs
 * @noextend This class is not intended to be subclassed by clients.
 */
public class EMFBaseIndexStatisticsStore extends AbstractBaseIndexStore {

    /**
     * A common map is used to store instance/value statistics. The key can be an {@link EClassifier}, 
     * {@link EStructuralFeature} or a String ID. 
     */
    private final Map<Object, Integer> stats = new HashMap<Object, Integer>();

    public EMFBaseIndexStatisticsStore(NavigationHelperImpl navigationHelper, Logger logger) {
        super(navigationHelper, logger);
    }
    public void addFeature(Object element, Object feature){
        addInstance(feature);
    }
    
    public void removeFeature(Object element, Object feature){
        removeInstance(feature);
    }
    
    public void addInstance(Object key){
        Integer v = stats.get(key);
        stats.put(key, v == null ? 1 : v+1);
    }
    
    public void removeInstance(Object key){
        Integer v = stats.get(key);
        if(v == null || v <= 0) {
            navigationHelper.logIncidentStatRemoval(key);
            return;
        }
        if (v.intValue() == 1){
            stats.remove(key);
        }else{
            stats.put(key, v-1);
        }
    }
    public int countInstances(Object key){
        Integer v = stats.get(key);
        return v == null ? 0 : v.intValue();
    }
    
    public void removeType(Object key){
        stats.remove(key);
    }

    public int countFeatures(Object feature) {
        return countInstances(feature);
    }
    
}
