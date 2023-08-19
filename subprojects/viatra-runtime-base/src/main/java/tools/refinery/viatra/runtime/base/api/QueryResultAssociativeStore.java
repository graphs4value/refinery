/*******************************************************************************
 * Copyright (c) 2010-2013, Abel Hegedus, Istvan Rath and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.viatra.runtime.base.api;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import tools.refinery.viatra.runtime.matchers.util.Direction;

/**
 * @author Abel Hegedus
 *
 */
public abstract class QueryResultAssociativeStore<KeyType, ValueType> {
    /**
     * Error literal returned when associative store modification is attempted without a setter available
     */
    protected static final String NOT_ALLOW_MODIFICATIONS = "Query result associative store does not allow modifications";
    
    /**
     * Logger that can be used for reporting errors during runtime
     */
    private Logger logger;
    /**
     * The collection of listeners registered for this result associative store
     */
    private Collection<IQueryResultUpdateListener<KeyType, ValueType>> listeners;

    /**
     * The setter registered for changing the contents of the associative store
     */
    private IQueryResultSetter<KeyType, ValueType> setter;

    /**
     * @return the listeners
     */
    protected Collection<IQueryResultUpdateListener<KeyType, ValueType>> getListeners() {
        return listeners;
    }

    /**
     * @param listeners the listeners to set
     */
    protected void setListeners(Collection<IQueryResultUpdateListener<KeyType, ValueType>> listeners) {
        this.listeners = listeners;
    }

    /**
     * @return the setter
     */
    protected IQueryResultSetter<KeyType, ValueType> getSetter() {
        return setter;
    }

    /**
     * @param setter the setter to set
     */
    protected void setSetter(IQueryResultSetter<KeyType, ValueType> setter) {
        this.setter = setter;
    }

    /**
     * @param logger the logger to set
     */
    protected void setLogger(Logger logger) {
        this.logger = logger;
    }

    /**
     * Returns the entries in the cache as a collection.
     * @return the entries
     */
    protected abstract Collection<Entry<KeyType, ValueType>> getCacheEntries();
    
    /**
     * Registers a listener for this query result associative store that is invoked every time when a key-value pair is inserted
     * or removed from the associative store.
     * 
     * <p>
     * The listener can be unregistered via {@link #removeCallbackOnQueryResultUpdate(IQueryResultUpdateListener)}.
     * 
     * @param listener
     *            the listener that will be notified of each key-value pair that is inserted or removed, starting from
     *            now.
     * @param fireNow
     *            if true, notifyPut will be immediately invoked on all current key-values as a one-time effect.
     */
    public void addCallbackOnQueryResultUpdate(IQueryResultUpdateListener<KeyType, ValueType> listener, boolean fireNow) {
        if (listeners == null) {
            listeners = new HashSet<IQueryResultUpdateListener<KeyType, ValueType>>();
        }
        listeners.add(listener);
        if(fireNow) {
            for (Entry<KeyType, ValueType> entry : getCacheEntries()) {
                sendNotificationToListener(Direction.INSERT, entry.getKey(), entry.getValue(), listener);
            }
        }
    }
    
    /**
     * Unregisters a callback registered by {@link #addCallbackOnQueryResultUpdate(IQueryResultUpdateListener, boolean)}
     * .
     * 
     * @param listener
     *            the listener that will no longer be notified.
     */
    public void removeCallbackOnQueryResultUpdate(IQueryResultUpdateListener<KeyType, ValueType> listener) {
        if (listeners != null) {
            listeners.remove(listener);
        }
    }

    /**
     * This method notifies the listeners that the query result associative store has changed.
     * 
     * @param direction
     *            the type of the change (insert or delete)
     * @param key
     *            the key of the pair that changed
     * @param value
     *            the value of the pair that changed
     */
    protected void notifyListeners(Direction direction, KeyType key, ValueType value) {
        if(listeners != null) {
            for (IQueryResultUpdateListener<KeyType, ValueType> listener : listeners) {
                sendNotificationToListener(direction, key, value, listener);
            }
        }
    }

    private void sendNotificationToListener(Direction direction, KeyType key, ValueType value,
            IQueryResultUpdateListener<KeyType, ValueType> listener) {
        try {
            if (direction == Direction.INSERT) {
                listener.notifyPut(key, value);
            } else {
                listener.notifyRemove(key, value);
            }
        } catch (Exception e) { // NOPMD
            logger.warn(
                    String.format(
                            "The query result associative store encountered an error during executing a callback on %s of key %s and value %s. Error message: %s. (Developer note: %s in %s called from QueryResultMultimap)",
                            direction == Direction.INSERT ? "insertion" : "removal", key, value, e.getMessage(), e
                                    .getClass().getSimpleName(), listener), e);
            throw new IllegalStateException("The query result associative store encountered an error during invoking setter",e);
        }
    }
    
    /**
     * Implementations of QueryResultAssociativeStore can put a new key-value pair into the associative store with this method. If the
     * insertion of the key-value pair results in a change, the listeners are notified.
     * 
     * <p>
     * No validation or null-checking is performed during the method!
     * 
     * @param key
     *            the key which identifies where the new value is put
     * @param value
     *            the value that is put into the collection of the key
     * @return true, if the insertion resulted in a change (the key-value pair was not yet in the associative store)
     */
    protected boolean internalPut(KeyType key, ValueType value){
        boolean putResult = internalCachePut(key, value);
        if (putResult) {
            notifyListeners(Direction.INSERT, key, value);
        }
        return putResult;
    }
    /**
     * Implementations of QueryResultAssociativeStore can remove a key-value pair from the associative store with this method. If the
     * removal of the key-value pair results in a change, the listeners are notified.
     * 
     * <p>
     * No validation or null-checking is performed during the method!
     * 
     * @param key
     *            the key which identifies where the value is removed from
     * @param value
     *            the value that is removed from the collection of the key
     * @return true, if the removal resulted in a change (the key-value pair was in the associative store)
     */
    protected boolean internalRemove(KeyType key, ValueType value){
        boolean removeResult = internalCacheRemove(key, value);
        if (removeResult) {
            notifyListeners(Direction.DELETE, key, value);
        }
        return removeResult;
    }
    
    
    protected abstract boolean internalCachePut(KeyType key, ValueType value);
    protected abstract boolean internalCacheRemove(KeyType key, ValueType value);
    protected abstract int internalCacheSize();
    protected abstract boolean internalCacheContainsEntry(KeyType key, ValueType value);
    
    /**
     * @param setter
     *            the setter to set
     */
    public void setQueryResultSetter(IQueryResultSetter<KeyType, ValueType> setter) {
        this.setter = setter;
    }
    
    /**
     * @return the logger
     */
    protected Logger getLogger() {
        return logger;
    }
    
    protected void internalClear() {
        if (setter == null) {
            throw new UnsupportedOperationException(NOT_ALLOW_MODIFICATIONS);
        }
        Collection<Entry<KeyType, ValueType>> entries = new ArrayList<>(getCacheEntries());
        Iterator<Entry<KeyType, ValueType>> iterator = entries.iterator();
        while (iterator.hasNext()) {
            Entry<KeyType, ValueType> entry = iterator.next();
            modifyThroughQueryResultSetter(entry.getKey(), entry.getValue(), Direction.DELETE);
        }
        if (internalCacheSize() != 0) {
            StringBuilder sb = new StringBuilder();
            for (Entry<KeyType, ValueType> entry : getCacheEntries()) {
                if (sb.length() > 0) {
                    sb.append(", ");
                }
                sb.append(entry.toString());
            }
            logger.warn(String
                    .format("The query result associative store is not empty after clear, remaining entries: %s. (Developer note: %s called from QueryResultMultimap)",
                            sb.toString(), setter));
        }
    }
    
    /**
     * This method is used for calling the query result setter to put or remove a value by modifying the model.
     * 
     * <p>
     * The given key-value pair is first validated (see {@link IQueryResultSetter#validate(Object, Object)}, then the
     * put or remove method is called (see {@link IQueryResultSetter#put(Object, Object)} and
     * {@link IQueryResultSetter#remove(Object, Object)}). If the setter reported that the model has been changed, the
     * change is checked.
     * 
     * <p>
     * If the model modification did not change the result set in the desired way, a warning is logged.
     * 
     * <p>
     * If the setter throws any {@link Throwable}, it is either rethrown in case of {@link Error} and logged otherwise.
     * 
     * 
     * @param key
     *            the key of the pair to be inserted or removed
     * @param value
     *            the value of the pair to be inserted or removed
     * @param direction
     *            specifies whether a put or a remove is performed
     * @return true, if the associative store changed according to the direction
     */
    protected boolean modifyThroughQueryResultSetter(KeyType key, ValueType value, Direction direction) {
        try {
            if (setter.validate(key, value)) {
                final int size = internalCacheSize();
                final int expectedChange = (direction == Direction.INSERT) ? 1 : -1;
                boolean changed = false;
                if (direction == Direction.INSERT) {
                    changed = setter.put(key, value);
                } else {
                    changed = setter.remove(key, value);
                }
                if (changed) {
                    return checkModificationThroughQueryResultSetter(key, value, direction, expectedChange, size);
                } else {
                    logger.warn(String
                            .format("The query result associative store %s of key %s and value %s resulted in %s. (Developer note: %s called from QueryResultMultimap)",
                                    direction == Direction.INSERT ? "insertion" : "removal", key, value,
                                    Math.abs(internalCacheSize() - size) > 1 ? "more than one changed result"
                                            : "no changed results", setter));
                }
            }
        } catch (Exception e) { // NOPMD
            logger.warn(
                    String.format(
                            "The query result associative store encountered an error during invoking setter on %s of key %s and value %s. Error message: %s. (Developer note: %s in %s called from QueryResultMultimap)",
                            direction == Direction.INSERT ? "insertion" : "removal", key, value, e.getMessage(), e
                                    .getClass().getSimpleName(), setter), e);
            throw new IllegalStateException("The query result associative store encountered an error during invoking setter",e);
        }

        return false;
    }

    /**
     * Checks whether the model modification performed by the {@link IQueryResultSetter} resulted in the insertion or
     * removal of exactly the required key-value pair.
     * 
     * @param key
     *            the key for the pair that was inserted or removed
     * @param value
     *            the value for the pair that was inserted or removed
     * @param direction
     *            the direction of the change
     * @param size
     *            the size of the cache before the change
     * @return true, if the changes made by the query result setter were correct
     */
    protected boolean checkModificationThroughQueryResultSetter(KeyType key, ValueType value, Direction direction,
            final int expectedChange, final int size) {
        boolean isInsertion = direction == Direction.INSERT;
        return (isInsertion == internalCacheContainsEntry(key, value)
                && (internalCacheSize() - expectedChange) == size);
    }
}
