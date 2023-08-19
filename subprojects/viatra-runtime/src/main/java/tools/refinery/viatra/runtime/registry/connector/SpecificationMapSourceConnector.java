/*******************************************************************************
 * Copyright (c) 2010-2016, Abel Hegedus, IncQuery Labs Ltd.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.viatra.runtime.registry.connector;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;

import tools.refinery.viatra.runtime.extensibility.IQuerySpecificationProvider;
import tools.refinery.viatra.runtime.extensibility.SingletonQuerySpecificationProvider;
import tools.refinery.viatra.runtime.registry.IConnectorListener;

/**
 * A simple connector implementation that allows users to simply add and remove specifications. These changes are
 * propagated to listeners (e.g. the registry). Note that duplicate FQNs are not allowed in a given connector.
 * 
 * @author Abel Hegedus
 * @since 1.3
 *
 */
public class SpecificationMapSourceConnector extends AbstractRegistrySourceConnector {

    private static final String DUPLICATE_MESSAGE = "Duplicate FQN %s cannot be added to connector";
    private Map<String, IQuerySpecificationProvider> specificationProviderMap;

    /**
     * Creates an instance of the connector with the given identifier. The identifier should be unique if you want to
     * add it to a registry as a source.
     * 
     * @param identifier
     *            of the newly created connector
     * @param includeInDefaultViews
     *            true if the specifications in the connector should be included in default views
     */
    public SpecificationMapSourceConnector(String identifier, boolean includeInDefaultViews) {
        super(identifier, includeInDefaultViews);
        this.specificationProviderMap = new HashMap<>();
    }
    
    /**
     * Creates an instance of the connector with the given identifier and fills it up with the given specification
     * providers. The identifier should be unique if you want to add it to a registry as a source.
     * 
     * @param identifier
     *            of the newly created connector
     * @param specificationProviders
     *            the initial set of specifications in the connector
     * @param includeInDefaultViews
     *            true if the specifications in the connector should be included in default views
     */
    public SpecificationMapSourceConnector(String identifier, Set<IQuerySpecificationProvider> specificationProviders, boolean includeInDefaultViews) {
        this(identifier, includeInDefaultViews);
        for (IQuerySpecificationProvider provider : specificationProviders) {
            addQuerySpecificationProvider(provider);
        }
    }

    /**
     * Creates an instance of the connector with the given identifier and fills it up with the specification providers
     * from the given {@link SpecificationMapSourceConnector}. The identifier should be unique if you want to add it to
     * a registry as a source.
     * 
     * @param identifier
     *            of the newly created connector
     * @param connector
     *            that contains the specifications to copy into the new instance
     * @param includeInDefaultViews
     *            true if the specifications in the connector should be included in default views
     */
    public SpecificationMapSourceConnector(String identifier, SpecificationMapSourceConnector connector, boolean includeInDefaultViews) {
        this(identifier, includeInDefaultViews);
        this.specificationProviderMap.putAll(connector.specificationProviderMap);
    }

    /**
     * Adds a query specification to the connector.
     * If you have an {@link IQuerySpecification} object, use {@link SingletonQuerySpecificationProvider}.
     * 
     * @param provider to add to the connector
     * @throws IllegalArgumentException if the connector already contains a specification with the same FQN
     */
    public void addQuerySpecificationProvider(IQuerySpecificationProvider provider) {
        Objects.requireNonNull(provider, "Provider must not be null!");
        String fullyQualifiedName = provider.getFullyQualifiedName();
        if (!specificationProviderMap.containsKey(fullyQualifiedName)) {
            specificationProviderMap.put(fullyQualifiedName, provider);
            for (IConnectorListener listener : listeners) {
                listener.querySpecificationAdded(this, provider);
            }
        } else {
            throw new IllegalArgumentException(String.format(DUPLICATE_MESSAGE, fullyQualifiedName));
        }
    }

    /**
     * Remove a specification that has been added with the given FQN.
     * 
     * @param fullyQualifiedName
     * @throws NoSuchElementException if the connector does not contain a specification with the given FQN
     */
    public void removeQuerySpecificationProvider(String fullyQualifiedName) {
        Objects.requireNonNull(fullyQualifiedName, "Fully qualified name must not be null!");
        IQuerySpecificationProvider provider = specificationProviderMap.remove(fullyQualifiedName);
        if (provider == null) {
            throw new NoSuchElementException(
                    String.format("Connector does not contain specification with FQN %s", fullyQualifiedName));
        }
        for (IConnectorListener listener : listeners) {
            listener.querySpecificationRemoved(this, provider);
        }
    }

    /**
     * @return the immutable copy of the set of FQNs for the added query specifications
     */
    public Set<String> getQuerySpecificationFQNs() {
        return Collections.unmodifiableSet(new HashSet<>(specificationProviderMap.keySet()));
    }

    /**
     * 
     * @param fullyQualifiedName that is checked
     * @return true if a specification with the given FQN exists in the connector, false otherwise
     */
    public boolean hasQuerySpecificationFQN(String fullyQualifiedName) {
        Objects.requireNonNull(fullyQualifiedName, "FQN must not be null!");
        return specificationProviderMap.containsKey(fullyQualifiedName);
    }

    @Override
    protected void sendQuerySpecificationsToListener(IConnectorListener listener) {
        for (IQuerySpecificationProvider provider : specificationProviderMap.values()) {
            listener.querySpecificationAdded(this, provider);
        }
    }

}
