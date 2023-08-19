/*******************************************************************************
 * Copyright (c) 2010-2012, Abel Hegedus, Istvan Rath and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.viatra.runtime.base.api;

/**
 * Setter interface for query result multimaps that allow modifications of the model through the multimap.
 * 
 * <p>
 * The model modifications should ensure that the multimap changes exactly as required (i.e. a put results in only one
 * new key-value pair and remove results in only one removed pair).
 * 
 * <p>
 * The input parameters of both put and remove can be validated by implementing the {@link #validate(Object, Object)}
 * method.
 * 
 * @author Abel Hegedus
 * 
 * @param <KeyType>
 * @param <ValueType>
 */
public interface IQueryResultSetter<KeyType, ValueType> {
    /**
     * Modify the underlying model of the query in order to have the given key-value pair as a new result of the query.
     * 
     * @param key
     *            the key for which a new value is added to the query results
     * @param value
     *            the new value that should be added to the query results for the given key
     * @return true, if the query result changed
     */
    boolean put(KeyType key, ValueType value);

    /**
     * Modify the underlying model of the query in order to remove the given key-value pair from the results of the
     * query.
     * 
     * @param key
     *            the key for which the value is removed from the query results
     * @param value
     *            the value that should be removed from the query results for the given key
     * @return true, if the query result changed
     */
    boolean remove(KeyType key, ValueType value);

    /**
     * Validates a given key-value pair for the query result. The validation has to ensure that (1) if the pair does not
     * exist in the result, it can be added safely (2) if the pair already exists in the result, it can be removed
     * safely
     * 
     * @param key
     *            the key of the pair that is validated
     * @param value
     *            the value of the pair that is validated
     * @return true, if the pair does not exists but can be added or the pair exists and can be removed
     */
    boolean validate(KeyType key, ValueType value);
}