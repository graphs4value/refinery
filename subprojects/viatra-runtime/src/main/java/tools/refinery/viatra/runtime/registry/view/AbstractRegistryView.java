/*******************************************************************************
 * Copyright (c) 2010-2016, Abel Hegedus, IncQuery Labs Ltd.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.viatra.runtime.registry.view;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;
import tools.refinery.viatra.runtime.matchers.util.CollectionsFactory;
import tools.refinery.viatra.runtime.matchers.util.CollectionsFactory.MemoryType;
import tools.refinery.viatra.runtime.matchers.util.IMemoryView;
import tools.refinery.viatra.runtime.matchers.util.IMultiLookup;
import tools.refinery.viatra.runtime.matchers.util.Preconditions;
import tools.refinery.viatra.runtime.registry.IQuerySpecificationRegistry;
import tools.refinery.viatra.runtime.registry.IQuerySpecificationRegistryChangeListener;
import tools.refinery.viatra.runtime.registry.IQuerySpecificationRegistryEntry;
import tools.refinery.viatra.runtime.registry.IRegistryView;
import tools.refinery.viatra.runtime.util.ViatraQueryLoggingUtil;

/**
 * An abstract {@link IRegistryView} implementation that stores the registry, the set of listeners added to the view and
 * the FQN to entry map of the view itself. The only responsibility of subclasses is to decide whether an entry received
 * as an addition or removal notification is relevant to the view.
 * 
 * @author Abel Hegedus
 * @since 1.3
 */
public abstract class AbstractRegistryView implements IRegistryView {

    private static final String LISTENER_EXCEPTION_REMOVE = "Exception occurred while notifying view listener %s about entry removal";
    private static final String LISTENER_EXCEPTION_ADD = "Exception occurred while notifying view listener %s about entry addition";
    protected final IQuerySpecificationRegistry registry;
    protected final IMultiLookup<String, IQuerySpecificationRegistryEntry> fqnToEntryMap;
    protected final Set<IQuerySpecificationRegistryChangeListener> listeners;
    protected final boolean allowDuplicateFQNs;

    /**
     * This method is called both when an addition or removal notification is received from the registry. Subclasses can
     * implement view filtering by returning false for those specifications that are not relevant for this view.
     * 
     * @param entry
     *            that is added or removed in the registry
     * @return true if the entry should be added to or removed from the view, false otherwise
     */
    protected abstract boolean isEntryRelevant(IQuerySpecificationRegistryEntry entry);

    /**
     * Creates a new view instance for the given registry. Note that views are created by the registry and the view
     * update mechanisms are also set up by the registry.
     * 
     * @param registry
     */
    public AbstractRegistryView(IQuerySpecificationRegistry registry, boolean allowDuplicateFQNs) {
        this.registry = registry;
        this.allowDuplicateFQNs = allowDuplicateFQNs;
        this.fqnToEntryMap = CollectionsFactory.createMultiLookup(Object.class, MemoryType.SETS, Object.class);
        this.listeners = new HashSet<>();
    }

    @Override
    public IQuerySpecificationRegistry getRegistry() {
        return registry;
    }

    @Override
    public Iterable<IQuerySpecificationRegistryEntry> getEntries() {
        return fqnToEntryMap.distinctValuesStream().collect(Collectors.toSet());
    }

    @Override
    public Set<String> getQuerySpecificationFQNs() {
        return fqnToEntryMap.distinctKeysStream().collect(Collectors.toSet());
    }

    @Override
    public boolean hasQuerySpecificationFQN(String fullyQualifiedName) {
        Preconditions.checkArgument(fullyQualifiedName != null, "FQN must not be null!");
        return fqnToEntryMap.lookupExists(fullyQualifiedName);
    }

    @Override
    public Set<IQuerySpecificationRegistryEntry> getEntries(String fullyQualifiedName) {
        Preconditions.checkArgument(fullyQualifiedName != null, "FQN must not be null!");
        IMemoryView<IQuerySpecificationRegistryEntry> entries = fqnToEntryMap.lookupOrEmpty(fullyQualifiedName);
        return Collections.unmodifiableSet(entries.distinctValues());
    }

    @Override
    public void addViewListener(IQuerySpecificationRegistryChangeListener listener) {
        Preconditions.checkArgument(listener != null, "Null listener not supported");
        listeners.add(listener);
    }

    @Override
    public void removeViewListener(IQuerySpecificationRegistryChangeListener listener) {
        Preconditions.checkArgument(listener != null, "Null listener not supported");
        listeners.remove(listener);
    }

    @Override
    public void entryAdded(IQuerySpecificationRegistryEntry entry) {
        if (isEntryRelevant(entry)) {
            String fullyQualifiedName = entry.getFullyQualifiedName();
            IMemoryView<IQuerySpecificationRegistryEntry> duplicates = fqnToEntryMap.lookup(fullyQualifiedName);
            if(!allowDuplicateFQNs && duplicates != null) {
                Set<IQuerySpecificationRegistryEntry> removed = new HashSet<>(duplicates.distinctValues());
                for (IQuerySpecificationRegistryEntry e : removed) {
                    fqnToEntryMap.removePair(fullyQualifiedName, e);
                    notifyListeners(e, false);
                }
            }
            fqnToEntryMap.addPair(fullyQualifiedName, entry);
            notifyListeners(entry, true);
        }
    }

    @Override
    public void entryRemoved(IQuerySpecificationRegistryEntry entry) {
        if (isEntryRelevant(entry)) {
            String fullyQualifiedName = entry.getFullyQualifiedName();
            fqnToEntryMap.removePair(fullyQualifiedName, entry);
            notifyListeners(entry, false);
        }
    }

    private void notifyListeners(IQuerySpecificationRegistryEntry entry, boolean addition) {
        for (IQuerySpecificationRegistryChangeListener listener : listeners) {
            try {
                if(addition){
                    listener.entryAdded(entry);
                } else {
                    listener.entryRemoved(entry);
                }
            } catch (Exception ex) {
                Logger logger = ViatraQueryLoggingUtil.getLogger(AbstractRegistryView.class);
                String formatString = addition ? LISTENER_EXCEPTION_ADD : LISTENER_EXCEPTION_REMOVE;
                logger.error(String.format(formatString, listener), ex);
            }
        }
    }

}