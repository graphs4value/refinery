/*******************************************************************************
 * Copyright (c) 2010-2015, Bergmann Gabor, Istvan Rath and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.viatra.runtime.emf;

import org.apache.log4j.Logger;
import tools.refinery.viatra.runtime.base.api.NavigationHelper;
import tools.refinery.viatra.runtime.matchers.tuple.Tuple;
import tools.refinery.viatra.runtime.matchers.tuple.Tuples;

/**
 * In dynamic EMF mode, we need to make sure that EEnum literal constants and values returned by eval() expressions 
 * 	are canonicalized in the same way as enum literals from the EMF model.
 * 
 * <p> This canonicalization is a one-way mapping, so 
 * 	{@link #unwrapElement(Object)} and {@link #unwrapTuple(Object)} remain NOPs.
 * 
 * @author Bergmann Gabor
 *
 */
public class DynamicEMFQueryRuntimeContext extends EMFQueryRuntimeContext {

    public DynamicEMFQueryRuntimeContext(NavigationHelper baseIndex, Logger logger, EMFScope emfScope) {
        super(baseIndex, logger, emfScope);
    }	
    
    @Override
    public Object wrapElement(Object externalElement) {
        return baseIndex.toCanonicalValueRepresentation(externalElement);
    }
    
    @Override
    public Tuple wrapTuple(Tuple externalElements) {
        Object[] elements = externalElements.getElements();
        for (int i=0; i< elements.length; ++i)
            elements[i] = wrapElement(elements[i]);
        return Tuples.flatTupleOf(elements);
    }
    
    
    
}
