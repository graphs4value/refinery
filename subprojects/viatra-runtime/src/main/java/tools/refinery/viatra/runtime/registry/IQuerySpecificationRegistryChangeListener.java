/*******************************************************************************
 * Copyright (c) 2010-2016, Abel Hegedus, IncQuery Labs Ltd.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.viatra.runtime.registry;

/**
 * Listener interface for providing update notifications of views to users. It is used for propagating changes from the
 * query specification registry to the views and from the views to users.
 * 
 * @author Abel Hegedus
 * @since 1.3
 *
 */
public interface IQuerySpecificationRegistryChangeListener {

    /**
     * Called when a new entry is added to the registry.
     * 
     * @param entry that is added
     */
    void entryAdded(IQuerySpecificationRegistryEntry entry);

    /**
     * Called when an existing entry is removed from the registry.
     *  
     * @param entry that is removed
     */
    void entryRemoved(IQuerySpecificationRegistryEntry entry);

}
