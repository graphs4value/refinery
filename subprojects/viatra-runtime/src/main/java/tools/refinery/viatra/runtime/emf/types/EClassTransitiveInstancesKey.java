/*******************************************************************************
 * Copyright (c) 2010-2015, Bergmann Gabor, Istvan Rath and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.viatra.runtime.emf.types;

import org.eclipse.emf.ecore.EClass;
import tools.refinery.viatra.runtime.emf.EMFScope;
import tools.refinery.viatra.runtime.emf.helper.ViatraQueryRuntimeHelper;

/**
 * Instance tuples are of form (x), where x is an eObject instance of the given eClass or one of its subclasses <b>within the scope</b>.
 * <p> As of version 1.6, this input key has the strict semantics that instances must be within the {@link EMFScope}.
 * @author Bergmann Gabor
 *
 */
public class EClassTransitiveInstancesKey extends BaseEMFTypeKey<EClass> {

    public EClassTransitiveInstancesKey(EClass emfKey) {
        super(emfKey);
    }

    @Override
    public String getPrettyPrintableName() {
        return "(scoped) "+ViatraQueryRuntimeHelper.prettyPrintEMFType(wrappedKey);
    }

    @Override
    public String getStringID() {
        return "eClass(scoped)#"+ ViatraQueryRuntimeHelper.prettyPrintEMFType(wrappedKey);
    }

    @Override
    public int getArity() {
        return 1;
    }
    
    @Override
    public boolean isEnumerable() {
        return true;
    }

}
