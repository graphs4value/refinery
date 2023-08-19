/*******************************************************************************
 * Copyright (c) 2010-2013, Abel Hegedus, Istvan Rath and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.viatra.runtime.base.api;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import tools.refinery.viatra.runtime.matchers.util.Direction;

/**
 * @author Abel Hegedus
 *
 */
public abstract class QueryResultMap<KeyType,ValueType> extends QueryResultAssociativeStore<KeyType, ValueType> implements Map<KeyType, ValueType> {

    /**
     * This map contains the current key-values. Implementing classes should not modify it directly
     */
    private Map<KeyType, ValueType> cache;
    
    /**
     * Constructor only visible to subclasses.
     * 
     * @param logger
     *            a logger that can be used for error reporting
     */
    protected QueryResultMap(Logger logger) {
        cache = new HashMap<KeyType, ValueType>();
        setLogger(logger);
    }
    
    @Override
    protected Collection<java.util.Map.Entry<KeyType, ValueType>> getCacheEntries() {
        return cache.entrySet();
    }

    @Override
    protected boolean internalCachePut(KeyType key, ValueType value) {
        ValueType put = cache.put(key, value);
        if(put == null) {
            return value != null;
        } else {
            return !put.equals(value);
        }
    }
    
    @Override
    protected boolean internalCacheRemove(KeyType key, ValueType value) {
        ValueType remove = cache.remove(key);
        return remove != null;
    }

    @Override
    protected int internalCacheSize() {
        return cache.size();
    }
    
    @Override
    protected boolean internalCacheContainsEntry(KeyType key, ValueType value) {
        return cache.containsKey(key) && cache.get(key).equals(value);
    }
    
    /**
     * @return the cache
     */
    protected Map<KeyType, ValueType> getCache() {
        return cache;
    }

    /**
     * @param cache
     *            the cache to set
     */
    protected void setCache(Map<KeyType, ValueType> cache) {
        this.cache = cache;
    }
    
    // ======================= implemented Map methods ======================

    @Override
    public void clear() {
        internalClear();
    }

    @Override
    public boolean containsKey(Object key) {
        return cache.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        return cache.containsValue(value);
    }

    /**
     * {@inheritDoc}
     * 
     * <p>
     * The returned set is immutable.
     * 
     */
    @Override
    public Set<Entry<KeyType, ValueType>> entrySet() {
        return Collections.unmodifiableSet((Set<Entry<KeyType, ValueType>>) getCacheEntries());
    }

    @Override
    public ValueType get(Object key) {
        return cache.get(key);
    }

    @Override
    public boolean isEmpty() {
        return cache.isEmpty();
    }

    /**
     * {@inheritDoc}
     * 
     * <p>
     * The returned set is immutable.
     * 
     */
    @Override
    public Set<KeyType> keySet() {
        return Collections.unmodifiableSet(cache.keySet());
    }

    /**
     * {@inheritDoc}
     * 
     * <p>
     * Throws {@link UnsupportedOperationException} if there is no {@link IQueryResultSetter}
     */
    @Override
    public ValueType put(KeyType key, ValueType value) {
        if (getSetter() == null) {
            throw new UnsupportedOperationException(NOT_ALLOW_MODIFICATIONS);
        }
        ValueType oldValue = cache.get(key);
        boolean modified = modifyThroughQueryResultSetter(key, value, Direction.INSERT);
        return modified ? oldValue : null;
    }

    /**
     * {@inheritDoc}
     * 
     * <p>
     * Throws {@link UnsupportedOperationException} if there is no {@link IQueryResultSetter}
     */
    @Override
    public void putAll(Map<? extends KeyType, ? extends ValueType> map) {
        if (getSetter() == null) {
            throw new UnsupportedOperationException(NOT_ALLOW_MODIFICATIONS);
        }
        for (Entry<? extends KeyType, ? extends ValueType> entry : map.entrySet()) {
            modifyThroughQueryResultSetter(entry.getKey(), entry.getValue(), Direction.INSERT);
        }
        return;
    }

    /**
     * {@inheritDoc}
     * 
     * <p>
     * Throws {@link UnsupportedOperationException} if there is no {@link IQueryResultSetter}
     */
    @SuppressWarnings("unchecked")
    @Override
    public ValueType remove(Object key) {
        if (getSetter() == null) {
            throw new UnsupportedOperationException(NOT_ALLOW_MODIFICATIONS);
        }
        // if it contains the entry, the types MUST be correct
        if (cache.containsKey(key)) {
            ValueType value = cache.get(key);
            modifyThroughQueryResultSetter((KeyType) key, value, Direction.DELETE);
            return value;
        }
        return null;
    }

    @Override
    public int size() {
        return internalCacheSize();
    }

    /**
     * {@inheritDoc}
     * 
     * <p>
     * The returned collection is immutable.
     * 
     */
    @Override
    public Collection<ValueType> values() {
        return Collections.unmodifiableCollection(cache.values());
    }

}
