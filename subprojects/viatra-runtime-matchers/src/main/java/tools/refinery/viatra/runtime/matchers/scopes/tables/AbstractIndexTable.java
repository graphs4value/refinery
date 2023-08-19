/*******************************************************************************
 * Copyright (c) 2010-2018, Gabor Bergmann, IncQuery Labs Ltd.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.viatra.runtime.matchers.scopes.tables;

import java.util.List;

import tools.refinery.viatra.runtime.matchers.context.IInputKey;
import tools.refinery.viatra.runtime.matchers.context.IQueryRuntimeContextListener;
import tools.refinery.viatra.runtime.matchers.tuple.Tuple;
import tools.refinery.viatra.runtime.matchers.tuple.TupleMask;
import tools.refinery.viatra.runtime.matchers.tuple.Tuples;
import tools.refinery.viatra.runtime.matchers.util.CollectionsFactory;
import tools.refinery.viatra.runtime.matchers.util.CollectionsFactory.MemoryType;
import tools.refinery.viatra.runtime.matchers.util.IMultiLookup;

/**
 * <p>
 * <strong>EXPERIMENTAL</strong>. This class or interface has been added as
 * part of a work in progress. There is no guarantee that this API will
 * work or that it will remain the same.
 *
 * @since 2.0
 * @author Gabor Bergmann
 */
public abstract class AbstractIndexTable implements IIndexTable {

    private IInputKey inputKey;
    protected ITableContext tableContext;
    
    protected final TupleMask emptyMask;
    protected final Tuple emptyTuple;


    public AbstractIndexTable(IInputKey inputKey, ITableContext tableContext) {
        this.inputKey = inputKey;
        this.tableContext = tableContext;
        
        this.emptyMask = TupleMask.empty(getInputKey().getArity());
        this.emptyTuple = Tuples.flatTupleOf(new Object[inputKey.getArity()]);
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + ":" + inputKey.getPrettyPrintableName();
    }

    @Override
    public IInputKey getInputKey() {
        return inputKey;
    }

    protected void logError(String message) {
        tableContext.logError(message);
    }
    
    
    /// UPDATE HANDLING SECTION
    
    // The entire mechanism is designed to accommodate a large number of update listeners,
    // but maybe there will typically be only a single, universal (unseeded) listener? 
    // TODO Create special handling for that case. 
    
    // Invariant: true iff #listenerGroupsAndSeed is nonempty
    protected boolean emitNotifications = false; 
    // Subscribed listeners grouped by their seed mask (e.g. all those that seed columns 2 and 5 are together), 
    // groups are stored in a list for quick delivery-time iteration (at the expense of adding / removing);
    // individual listeners can be looked up based on their seed tuple 
    protected List<IListenersWithSameMask> listenerGroups = CollectionsFactory.createObserverList();

    
    /**
     * Implementors shall call this to deliver all notifications.
     * Call may be conditioned to {@link #emitNotifications}
     */
    protected void deliverChangeNotifications(Tuple updateTuple, boolean isInsertion) {
        for (IListenersWithSameMask listenersForSeed : listenerGroups) {
            listenersForSeed.deliver(updateTuple, isInsertion);
        }
    }
    
    @Override
    public void addUpdateListener(Tuple seed, IQueryRuntimeContextListener listener) {
        TupleMask seedMask;
        if (seed == null) {
            seed = emptyTuple;
            seedMask = emptyMask;
        } else {
            seedMask = TupleMask.fromNonNullIndices(seed);
        }
        IListenersWithSameMask listenerGroup = getListenerGroup(seedMask);
        if (listenerGroup == null) { // create new group
            switch (seedMask.getSize()) {
            case 0:
                listenerGroup = new UniversalListeners();
                break;
            case 1: 
                listenerGroup = new ColumnBoundListeners(seedMask.indices[0]);
                break;
            default:
                listenerGroup = new GenericBoundListeners(seedMask);                        
            }
            listenerGroups.add(listenerGroup);
            emitNotifications = true;
        }
        listenerGroup.addUpdateListener(seed, listener);
    }

    @Override
    public void removeUpdateListener(Tuple seed, IQueryRuntimeContextListener listener) {
        TupleMask seedMask;
        if (seed == null) {
            seed = emptyTuple;
            seedMask = emptyMask;
        } else {
            seedMask = TupleMask.fromNonNullIndices(seed);
        }
        IListenersWithSameMask listenerGroup = getListenerGroup(seedMask);
        if (listenerGroup == null) 
            throw new IllegalStateException("no listener subscribed with mask" + seedMask);
        
        if (listenerGroup.removeUpdateListener(seed, listener)) {
            listenerGroups.remove(listenerGroup);
            emitNotifications = !listenerGroups.isEmpty();
        }
    }
    
    protected IListenersWithSameMask getListenerGroup(TupleMask seedMask) {
        for (IListenersWithSameMask candidateGroup : listenerGroups) { // group already exists?
            if (seedMask.equals(candidateGroup.getSeedMask())) {
                return candidateGroup;
            }
        }
        return null;
    }
    
    
    /**
     * Represents all listeners subscribed to seeds with the given seed mask.
     * 
     * @author Bergmann Gabor
     */
    protected static interface IListenersWithSameMask { 
        
        public TupleMask getSeedMask();
        
        public void deliver(Tuple updateTuple, boolean isInsertion);
        
        public void addUpdateListener(Tuple originalSeed, IQueryRuntimeContextListener listener);
        /**
         * @return true if this was the last listener, and the {@link IListenersWithSameMask} can be disposed of.
         */
        public boolean removeUpdateListener(Tuple originalSeed, IQueryRuntimeContextListener listener);
    }
    /** 
     * Listeners interested in all tuples
     */
    protected final class UniversalListeners implements IListenersWithSameMask {
        private final TupleMask mask = TupleMask.empty(inputKey.getArity());
        private List<IQueryRuntimeContextListener> listeners = CollectionsFactory.createObserverList();
        
        @Override
        public TupleMask getSeedMask() {
            return mask;
        }
        @Override
        public void deliver(Tuple updateTuple, boolean isInsertion) {
            IInputKey key = inputKey;
            for (IQueryRuntimeContextListener listener : listeners) {
                listener.update(key, updateTuple, isInsertion);
            }
        }
        @Override
        public void addUpdateListener(Tuple originalSeed, IQueryRuntimeContextListener listener) {
            listeners.add(listener);
        }
        @Override
        public boolean removeUpdateListener(Tuple originalSeed, IQueryRuntimeContextListener listener) {
            listeners.remove(listener);
            return listeners.isEmpty();
        }
    }
    /** 
     * Listeners interested in all tuples seeded by a single columns
     */
    protected final class ColumnBoundListeners implements IListenersWithSameMask {
        private int seedPosition;
        protected final TupleMask mask;
        // indexed by projected seed tuple
        protected IMultiLookup<Object,IQueryRuntimeContextListener> listeners = 
                CollectionsFactory.createMultiLookup(Object.class, MemoryType.SETS, Object.class);
        
        public ColumnBoundListeners(int seedPosition) {
            this.seedPosition = seedPosition;
            this.mask = TupleMask.selectSingle(seedPosition, inputKey.getArity());
        }
        
        @Override
        public TupleMask getSeedMask() {
            return mask;
        }
        @Override
        public void deliver(Tuple updateTuple, boolean isInsertion) {
            IInputKey key = inputKey;
            Object projectedSeed = updateTuple.get(seedPosition);
           for (IQueryRuntimeContextListener listener : listeners.lookupOrEmpty(projectedSeed)) {
                listener.update(key, updateTuple, isInsertion);
            }
        }
        @Override
        public void addUpdateListener(Tuple originalSeed, IQueryRuntimeContextListener listener) {
            Object projectedSeed = originalSeed.get(seedPosition);
            listeners.addPair(projectedSeed, listener);
        }
        @Override
        public boolean removeUpdateListener(Tuple originalSeed, IQueryRuntimeContextListener listener) {
            Object projectedSeed = originalSeed.get(seedPosition);
            listeners.removePair(projectedSeed, listener);
            return listeners.countKeys() == 0;
        }
    }
   /** 
     * Listeners interested in all tuples seeded by a tuple of values
     */
    protected final class GenericBoundListeners implements IListenersWithSameMask {
        protected final TupleMask mask;
        // indexed by projected seed tuple
        protected IMultiLookup<Tuple,IQueryRuntimeContextListener> listeners = 
                CollectionsFactory.createMultiLookup(Object.class, MemoryType.SETS, Object.class);
        
        public GenericBoundListeners(TupleMask mask) {
            this.mask = mask;
        }
        
        @Override
        public TupleMask getSeedMask() {
            return mask;
        }
        @Override
        public void deliver(Tuple updateTuple, boolean isInsertion) {
            IInputKey key = inputKey;
            Tuple projectedSeed = mask.transform(updateTuple);
            for (IQueryRuntimeContextListener listener : listeners.lookupOrEmpty(projectedSeed)) {
                listener.update(key, updateTuple, isInsertion);
            }
        }
        @Override
        public void addUpdateListener(Tuple originalSeed, IQueryRuntimeContextListener listener) {
            Tuple projectedSeed = mask.transform(originalSeed);
            listeners.addPair(projectedSeed, listener);
        }
        @Override
        public boolean removeUpdateListener(Tuple originalSeed, IQueryRuntimeContextListener listener) {
            Tuple projectedSeed = mask.transform(originalSeed);
            listeners.removePair(projectedSeed, listener);
            return listeners.countKeys() == 0;
        }
    }


}