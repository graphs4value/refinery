/*******************************************************************************
 * Copyright (c) 2010-2016, Abel Hegedus, IncQuery Labs Ltd.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.viatra.runtime.registry.impl;

import java.util.NoSuchElementException;
import java.util.Set;
import java.util.stream.Collectors;

import tools.refinery.viatra.runtime.api.IQueryGroup;
import tools.refinery.viatra.runtime.api.LazyLoadingQueryGroup;
import tools.refinery.viatra.runtime.registry.IDefaultRegistryView;
import tools.refinery.viatra.runtime.registry.IQuerySpecificationRegistry;
import tools.refinery.viatra.runtime.registry.IQuerySpecificationRegistryEntry;
import tools.refinery.viatra.runtime.registry.view.AbstractRegistryView;

/**
 * Registry view implementation that considers specifications relevant if they are included in default views.
 * 
 * @author Abel Hegedus
 *
 */
public class GlobalRegistryView extends AbstractRegistryView implements IDefaultRegistryView {
    
    /**
     * Creates a new instance of the global view.
     * 
     * @param registry that defines the view
     */
    public GlobalRegistryView(IQuerySpecificationRegistry registry) {
        super(registry, false);
    }
    
    @Override
    protected boolean isEntryRelevant(IQuerySpecificationRegistryEntry entry) {
        return entry.includeInDefaultViews();
    }
    
    @Override
    public IQueryGroup getQueryGroup() {
        Set<IQuerySpecificationRegistryEntry> allQueries = 
                fqnToEntryMap.distinctValuesStream().collect(Collectors.toSet());
        IQueryGroup queryGroup = LazyLoadingQueryGroup.of(allQueries); 
        return queryGroup;
    }

    @Override
    public IQuerySpecificationRegistryEntry getEntry(String fullyQualifiedName) {
        Set<IQuerySpecificationRegistryEntry> entries = getEntries(fullyQualifiedName);
        if(entries.isEmpty()){
            throw new NoSuchElementException("Cannot find entry with FQN " + fullyQualifiedName);
        }
        if(entries.size() > 1) {
            throw new IllegalStateException("Global view must never contain duplicated FQNs!");
        }
        IQuerySpecificationRegistryEntry entry = entries.iterator().next();
        return entry;
    }

}
