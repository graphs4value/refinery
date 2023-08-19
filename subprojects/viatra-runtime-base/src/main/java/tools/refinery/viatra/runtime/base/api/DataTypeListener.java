/*******************************************************************************
 * Copyright (c) 2010-2012, Tamas Szabo, Gabor Bergmann, Istvan Rath and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.viatra.runtime.base.api;

import org.eclipse.emf.ecore.EDataType;

/**
 * Interface for observing insertion and deletion of instances of data types.
 * 
 * @author Tamas Szabo
 * 
 */
public interface DataTypeListener {

    /**
     * Called when an instance of the given type is inserted.
     * 
     * @param type
     *            the {@link EDataType}
     * @param instance
     *            the instance of the data type
     * @param firstOccurrence
     * 			  true if this value was not previously present in the model
     */
    public void dataTypeInstanceInserted(EDataType type, Object instance, boolean firstOccurrence);

    /**
     * Called when an instance of the given type is deleted.
     * 
     * @param type
     *            the {@link EDataType}
     * @param instance
     *            the instance of the data type
     * @param lastOccurrence
     * 			  true if this value is no longer present in the model
     */
    public void dataTypeInstanceDeleted(EDataType type, Object instance, boolean lastOccurrence);
}
