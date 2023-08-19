/*******************************************************************************
 * Copyright (c) 2010-2012, Tamas Szabo, Gabor Bergmann, Istvan Rath and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.viatra.runtime.base.api;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;

/**
 * Interface for observing insertion / deletion of instances of EClass.
 * 
 * @author Tamas Szabo
 * 
 */
public interface InstanceListener {

    /**
     * Called when the given instance was added to the model.
     * 
     * @param clazz
     *            an EClass registered for this listener, for which a new instance (possibly an instance of a subclass) was inserted into the model
     * @param instance
     *            an EObject instance that was inserted into the model
     */
    public void instanceInserted(EClass clazz, EObject instance);

    /**
     * Called when the given instance was removed from the model.
     * 
     * @param clazz
     *            an EClass registered for this listener, for which an instance (possibly an instance of a subclass) was removed from the model
     * @param instance
     *            an EObject instance that was removed from the model
     */
    public void instanceDeleted(EClass clazz, EObject instance);
}
