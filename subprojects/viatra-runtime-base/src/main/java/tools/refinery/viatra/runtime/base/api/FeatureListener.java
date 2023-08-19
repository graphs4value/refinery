/*******************************************************************************
 * Copyright (c) 2010-2012, Tamas Szabo, Gabor Bergmann, Istvan Rath and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.viatra.runtime.base.api;

import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.EStructuralFeature;

/**
 * Interface for observing insertion and deletion of structural feature values ("settings"). (Works both for
 * single-valued and many-valued features.)
 * 
 * @author Tamas Szabo
 * 
 */
public interface FeatureListener {

    /**
     * Called when the given value is inserted into the given feature of the given host EObject.
     * 
     * @param host
     *            the host (holder) of the feature
     * @param feature
     *            the {@link EAttribute} or {@link EReference} instance
     * @param value
     *            the target of the feature
     */
    public void featureInserted(EObject host, EStructuralFeature feature, Object value);

    /**
     * Called when the given value is removed from the given feature of the given host EObject.
     * 
     * @param host
     *            the host (holder) of the feature
     * @param feature
     *            the {@link EAttribute} or {@link EReference} instance
     * @param value
     *            the target of the feature
     */
    public void featureDeleted(EObject host, EStructuralFeature feature, Object value);
}
