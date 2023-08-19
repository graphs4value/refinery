/*******************************************************************************
 * Copyright (c) 2010-2012, Tamas Szabo, Gabor Bergmann, Istvan Rath and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package tools.refinery.viatra.runtime.base.exception;

import tools.refinery.viatra.runtime.matchers.ViatraQueryRuntimeException;

public class ViatraBaseException extends ViatraQueryRuntimeException {

    private static final long serialVersionUID = -5145445047912938251L;

    public static final String EMPTY_REF_LIST = "At least one EReference must be provided!";
    public static final String INVALID_EMFROOT = "Emf navigation helper can only be attached on the contents of an EMF EObject, Resource, or ResourceSet.";

    public ViatraBaseException(String s) {
        super(s);
    }

}
