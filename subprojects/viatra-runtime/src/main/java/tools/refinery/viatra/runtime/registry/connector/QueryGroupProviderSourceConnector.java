/*******************************************************************************
 * Copyright (c) 2010-2016, Abel Hegedus, IncQuery Labs Ltd.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.viatra.runtime.registry.connector;

import java.util.Objects;

import tools.refinery.viatra.runtime.extensibility.IQueryGroupProvider;
import tools.refinery.viatra.runtime.extensibility.IQuerySpecificationProvider;
import tools.refinery.viatra.runtime.registry.IConnectorListener;

/**
 * Source connector implementation that uses a {@link IQueryGroupProvider} to provide a query specifications into the
 * registry. The query group can be later updated which triggers the removal of all specifications of the old group and
 * the addition of all specifications from the new group.
 * 
 * @author Abel Hegedus
 * @since 1.3
 *
 */
public class QueryGroupProviderSourceConnector extends AbstractRegistrySourceConnector {

    IQueryGroupProvider queryGroupProvider;

    /**
     * Creates an instance of the connector with the given identifier and the query group provider. The identifier
     * should be unique if you want to add it to a registry as a source.
     * 
     * @param identifier
     *            of the newly created connector
     * @param provider
     *            that contains the query specifications handled by the connector
     * @param includeInDefaultViews
     *            true if the specifications in the connector should be included in default views
     */
    public QueryGroupProviderSourceConnector(String identifier, IQueryGroupProvider provider, boolean includeInDefaultViews) {
        super(identifier, includeInDefaultViews);
        this.queryGroupProvider = provider;
    }

    /**
     * Update the query group of the connector, which triggers the removal of all specifications on the old group and
     * addition of all specifications in the given group.
     * 
     * @param queryGroupProvider
     *            the queryGroupProvider to set
     * @param includeInDefaultViews
     *            true if the specifications in the connector should be included in default views
     */
    public void setQueryGroupProvider(IQueryGroupProvider queryGroupProvider) {
        Objects.requireNonNull(queryGroupProvider, "Query group provider must not be null!");
        IQueryGroupProvider oldProvider = this.queryGroupProvider;

        for (IQuerySpecificationProvider specificationProvider : oldProvider.getQuerySpecificationProviders()) {
            for (IConnectorListener iConnectorListener : listeners) {
                iConnectorListener.querySpecificationRemoved(this, specificationProvider);
            }
        }
        for (IQuerySpecificationProvider specificationProvider : queryGroupProvider.getQuerySpecificationProviders()) {
            for (IConnectorListener iConnectorListener : listeners) {
                iConnectorListener.querySpecificationAdded(this, specificationProvider);
            }
        }

        this.queryGroupProvider = queryGroupProvider;
    }

    @Override
    protected void sendQuerySpecificationsToListener(IConnectorListener listener) {
        for (IQuerySpecificationProvider specificationProvider : queryGroupProvider.getQuerySpecificationProviders()) {
            listener.querySpecificationAdded(this, specificationProvider);
        }
    }

}
