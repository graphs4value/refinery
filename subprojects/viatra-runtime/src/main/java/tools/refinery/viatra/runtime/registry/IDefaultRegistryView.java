/*******************************************************************************
 * Copyright (c) 2010-2016, Abel Hegedus, IncQuery Labs Ltd.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.viatra.runtime.registry;

import java.util.NoSuchElementException;

import tools.refinery.viatra.runtime.api.IQueryGroup;

/**
 * The default registry view ensures that the fully qualified name of entries are unique and provides an additional
 * method for retrieving the query group of entries for easy initialization.
 * 
 * @author Abel Hegedus
 * @since 1.3
 *
 */
public interface IDefaultRegistryView extends IRegistryView {

    /**
     * @return a query group containing all query specifications
     */
    IQueryGroup getQueryGroup();
    
    /**
     * @param fullyQualifiedName
     *            of the entry that is requested
     * @return the entry with the given FQN
     * @throws NoSuchElementException if there is no such entry in the default view
     */
    IQuerySpecificationRegistryEntry getEntry(String fullyQualifiedName);
}
