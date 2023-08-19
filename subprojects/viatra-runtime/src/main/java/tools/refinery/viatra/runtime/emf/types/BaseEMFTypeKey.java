/*******************************************************************************
 * Copyright (c) 2010-2015, Bergmann Gabor, Istvan Rath and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.viatra.runtime.emf.types;

import tools.refinery.viatra.runtime.matchers.context.common.BaseInputKeyWrapper;

/**
 * Base class for EMF Type keys. 
 * @author Bergmann Gabor
 *
 */
public abstract class BaseEMFTypeKey<EMFKey> extends BaseInputKeyWrapper<EMFKey> {

    public BaseEMFTypeKey(EMFKey emfKey) {
        super(emfKey);
    }

    public EMFKey getEmfKey() {
        return getWrappedKey();
    }
    
    @Override
    public String toString() {
        return this.getPrettyPrintableName();
    }
    

}
