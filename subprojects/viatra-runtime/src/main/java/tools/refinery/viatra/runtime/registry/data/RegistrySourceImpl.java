/*******************************************************************************
 * Copyright (c) 2010-2016, Abel Hegedus, IncQuery Labs Ltd.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.viatra.runtime.registry.data;

import java.util.Map;
import java.util.TreeMap;

/**
 * Internal data storage object that represents a query specification source driven by a connector. The source must have
 * unique identifier that is copied from the connector. The source uses a FQN to entry map to manage registry entries.
 * 
 * @author Abel Hegedus
 *
 */
public class RegistrySourceImpl {

    private String identifier;
    private boolean includeInDefaultViews;
    private QuerySpecificationStore querySpecificationStore;
    private Map<String, RegistryEntryImpl> fqnToEntryMap;

    /**
     * Creates a new source with the given identifier and an empty entry map.
     * 
     * @param identifier
     *            for the source
     * @param querySpecificationStore
     *            that contains this source
     * @param includeInDefaultViews
     *            true if the entries of the source should be included in default views
     */
    public RegistrySourceImpl(String identifier, QuerySpecificationStore querySpecificationStore, boolean includeInDefaultViews) {
        this.identifier = identifier;
        this.includeInDefaultViews = includeInDefaultViews;
        this.querySpecificationStore = querySpecificationStore;
        this.fqnToEntryMap = new TreeMap<>();
    }

    /**
     * @return the identifier of the source
     */
    public String getIdentifier() {
        return identifier;
    }

    /**
     * @return true if the entries in the source should be included in default views
     */
    public boolean includeEntriesInDefaultViews() {
        return includeInDefaultViews;
    }
    
    /**
     * @return the store that contains the source
     */
    public QuerySpecificationStore getStore() {
        return querySpecificationStore;
    }

    /**
     * @return the live, modifiable FQN to entry map 
     */
    public Map<String, RegistryEntryImpl> getFqnToEntryMap() {
        return fqnToEntryMap;
    }

}
