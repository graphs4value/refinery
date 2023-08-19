/*******************************************************************************
 * Copyright (c) 2010-2015, Bergmann Gabor, Istvan Rath and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.viatra.runtime.emf.types;

import org.eclipse.emf.ecore.EDataType;
import tools.refinery.viatra.runtime.emf.helper.ViatraQueryRuntimeHelper;

/**
 * Instance tuples are of form (x), where x is an instance of the given eDataType residing at an attribute slot of an eObject in the model.
 * @author Bergmann Gabor
 *
 */
public class EDataTypeInSlotsKey extends BaseEMFTypeKey<EDataType> {

    /**
     * @param emfKey
     */
    public EDataTypeInSlotsKey(EDataType emfKey) {
        super(emfKey);
    }

    @Override
    public String getPrettyPrintableName() {
        return "(Attribute Slot Values: " + ViatraQueryRuntimeHelper.prettyPrintEMFType(wrappedKey) + ")";
    }

    @Override
    public String getStringID() {
        return "slotValue#" + ViatraQueryRuntimeHelper.prettyPrintEMFType(wrappedKey);
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
