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
 * Internal data storage object that represents a query specification registry with a set of sources driven by
 * connectors. The sources must have unique identifiers.
 * 
 * @author Abel Hegedus
 *
 */
public class QuerySpecificationStore {

    private Map<String, RegistrySourceImpl> sources;

    /**
     * Creates a new instance with an empty identifier to source map.
     */
    public QuerySpecificationStore() {
        this.sources = new TreeMap<>();
    }

    /**
     * @return the live, modifiable identifier to source map
     */
    public Map<String, RegistrySourceImpl> getSources() {
        return sources;
    }
}
