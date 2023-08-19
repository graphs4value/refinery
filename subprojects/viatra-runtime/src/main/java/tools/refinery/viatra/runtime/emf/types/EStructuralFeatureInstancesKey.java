/*******************************************************************************
 * Copyright (c) 2010-2015, Bergmann Gabor, Istvan Rath and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.viatra.runtime.emf.types;

import org.eclipse.emf.ecore.EStructuralFeature;
import tools.refinery.viatra.runtime.emf.EMFScope;
import tools.refinery.viatra.runtime.emf.helper.ViatraQueryRuntimeHelper;

/**
 * Instance tuples are of form (x, y), where x is an eObject that has y as the value of the given feature (or one of the values in case of multi-valued).
 * 
 * <p> As of version 1.6, this input key has the strict semantics that x must be within the {@link EMFScope}, scoping is <b>not</b> implied for y.
 * @author Bergmann Gabor
 *
 */
public class EStructuralFeatureInstancesKey extends BaseEMFTypeKey<EStructuralFeature> {
    
    public EStructuralFeatureInstancesKey(EStructuralFeature emfKey) {
        super(emfKey);
    }

    @Override
    public String getPrettyPrintableName() {
        return ViatraQueryRuntimeHelper.prettyPrintEMFType(wrappedKey);
    }

    @Override
    public String getStringID() {
        return "feature#"+ getPrettyPrintableName();
    }

    @Override
    public int getArity() {
        return 2;
    }
    
    @Override
    public boolean isEnumerable() {
        return true;
    }

}
