/*******************************************************************************
 * Copyright (c) 2010-2016, Abel Hegedus, IncQuery Labs Ltd.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.viatra.runtime.registry;

import java.util.Set;

/**
 * The registry view is the primary interface for users to interact with the query specifications in an
 * {@link IQuerySpecificationRegistry}. Views are created using the createView methods of registry and their content is
 * also dynamically updated by the registry.
 * 
 * The view contains a set of {@link IQuerySpecificationRegistryEntry} objects that can be used to access the query
 * specifications themselves through the get() method.
 * 
 * Users can check the contents of the view and add listeners to get notifications on view changes (added or removed
 * entries).
 * 
 * @author Abel Hegedus
 * @since 1.3
 *
 */
public interface IRegistryView extends IQuerySpecificationRegistryChangeListener {

    /**
     * @return an immutable copy of all entries found in the view
     */
    Iterable<IQuerySpecificationRegistryEntry> getEntries();

    /**
     * @return the set of FQNs for the query specifications in the view
     */
    Set<String> getQuerySpecificationFQNs();

    /**
     * @param fullyQualifiedName
     *            that is looked up in the view
     * @return true if the view contains an entry with given FQN, false otherwise
     */
    boolean hasQuerySpecificationFQN(String fullyQualifiedName);

    /**
     * @param fullyQualifiedName
     *            of the entries that are requested
     * @return the possible empty set of entries with the given FQN
     */
    Set<IQuerySpecificationRegistryEntry> getEntries(String fullyQualifiedName);

    /**
     * Adds a listener to the view that will be notified when an entry is added to or removed from the view.
     * 
     * @param listener that is added
     */
    void addViewListener(IQuerySpecificationRegistryChangeListener listener);

    /**
     * Removes a listener that was previously added to the view.
     * 
     * @param listener that is removed
     */
    void removeViewListener(IQuerySpecificationRegistryChangeListener listener);

    /**
     * @return the registry underlying the view
     */
    IQuerySpecificationRegistry getRegistry();
}
