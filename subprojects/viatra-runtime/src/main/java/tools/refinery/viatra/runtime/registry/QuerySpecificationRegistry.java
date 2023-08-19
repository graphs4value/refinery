/*******************************************************************************
 * Copyright (c) 2010-2016, Abel Hegedus, IncQuery Labs Ltd.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.viatra.runtime.registry;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import tools.refinery.viatra.runtime.registry.impl.QuerySpecificationRegistryImpl;

/**
 * Registry for query specifications that can be accessed using fully qualified names through views.
 * Additional query specifications can be added using {@link IRegistrySourceConnector}s.
 * 
 * <p>
 * When running as an OSGi plug-in, the generated query specifications registered through extensions are automatically loaded
 * into the registry by the {@link ExtensionBasedQuerySpecificationLoader} class.
 * 
 * @author Abel Hegedus
 * @since 1.3
 *
 */
public class QuerySpecificationRegistry implements IQuerySpecificationRegistry {

    private static final QuerySpecificationRegistry INSTANCE = new QuerySpecificationRegistry();

    /**
     * @return the singleton query specification registry instance
     */
    public static IQuerySpecificationRegistry getInstance() {
        return INSTANCE;
    }
    
    private final QuerySpecificationRegistryImpl internalRegistry;
    /**
     * Connectors that should not be immediately loaded into the registry.
     */
    private final Set<IRegistrySourceConnector> delayedConnectors;
    
    /**
     * Hidden constructor for singleton instance
     */
    protected QuerySpecificationRegistry() {
        this.internalRegistry = new QuerySpecificationRegistryImpl();
        this.delayedConnectors = new HashSet<>();
        
    }
    
    /**
     * @return the internal registry after adding delayed source connectors
     */
    protected IQuerySpecificationRegistry getInternalRegistry() {
        if(!delayedConnectors.isEmpty()) {
            final Iterator<IRegistrySourceConnector> it = delayedConnectors.iterator();
            while (it.hasNext()) {
                final IRegistrySourceConnector connector = it.next();
                internalRegistry.addSource(connector);
                it.remove();
            }
        }
        return internalRegistry;
    }
    
    /**
     * When the registry adds itself as a listener to connectors, it must send all specification providers
     * to the registry. However, when {@link ExtensionBasedQuerySpecificationLoader} is triggered during the
     * activation of the plugin, the individual query specification classes cannot be loaded yet. To avoid this, 
     * the connector of the loader is delayed until needed.
     * 
     * @param connector that should be delayed before adding to the registry
     */
    protected void addDelayedSourceConnector(IRegistrySourceConnector connector) {
        delayedConnectors.add(connector);
    }
    
    @Override
    public boolean addSource(IRegistrySourceConnector connector) {
        return getInternalRegistry().addSource(connector);
    }

    @Override
    public boolean removeSource(IRegistrySourceConnector connector) {
        return getInternalRegistry().removeSource(connector);
    }

    @Override
    public IDefaultRegistryView getDefaultView() {
        return getInternalRegistry().getDefaultView();
    }

    @Override
    public IRegistryView createView() {
        return getInternalRegistry().createView();
    }

    @Override
    public IRegistryView createView(IRegistryViewFilter filter) {
        return getInternalRegistry().createView(filter);
    }

    @Override
    public IRegistryView createView(IRegistryViewFactory factory) {
        return getInternalRegistry().createView(factory);
    }

}
