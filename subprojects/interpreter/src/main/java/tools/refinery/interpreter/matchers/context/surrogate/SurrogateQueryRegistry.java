/*******************************************************************************
 * Copyright (c) 2010-2015, Abel Hegedus, Zoltan Ujhelyi, Istvan Rath and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.interpreter.matchers.context.surrogate;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import tools.refinery.interpreter.matchers.psystem.queries.PQuery;
import tools.refinery.interpreter.matchers.context.IInputKey;
import tools.refinery.interpreter.matchers.util.IProvider;
import tools.refinery.interpreter.matchers.util.Preconditions;
import tools.refinery.interpreter.matchers.util.SingletonInstanceProvider;

/**
 * @author Abel Hegedus
 *
 */
public class SurrogateQueryRegistry {

    private Map<IInputKey, IProvider<PQuery>> registeredSurrogateQueryMap = new HashMap<>();
    private Map<IInputKey, IProvider<PQuery>> dynamicSurrogateQueryMap = new HashMap<>();

    /**
     * Hidden constructor
     */
    private SurrogateQueryRegistry() {
    }

    private static final SurrogateQueryRegistry INSTANCE = new SurrogateQueryRegistry();

    public static SurrogateQueryRegistry instance() {
        return INSTANCE;
    }

    /**
     *
     * @param feature
     * @param surrogateQuery
     * @return the previous surrogate query associated with feature, or null if there was no such query FQN registered
     * @throws IllegalArgumentException if feature or surrogateQuery is null
     */
    public IProvider<PQuery> registerSurrogateQueryForFeature(IInputKey feature, PQuery surrogateQuery) {
        Preconditions.checkArgument(surrogateQuery != null, "Surrogate query must not be null!");
        return registerSurrogateQueryForFeature(feature, new SingletonInstanceProvider<PQuery>(surrogateQuery));
    }

    /**
     *
     * @param feature
     * @param surrogateQuery
     * @return the previous surrogate query associated with feature, or null
     *         if there was no such query registered
     * @throws IllegalArgumentException
     *             if feature or surrogateQuery is null
     */
    public IProvider<PQuery> registerSurrogateQueryForFeature(IInputKey feature, IProvider<PQuery> surrogateQueryProvider) {
        Preconditions.checkArgument(feature != null, "Feature must not be null!");
        Preconditions.checkArgument(surrogateQueryProvider != null, "Surrogate query must not be null!");
        return registeredSurrogateQueryMap.put(feature, surrogateQueryProvider);
    }

    public IProvider<PQuery> addDynamicSurrogateQueryForFeature(IInputKey feature, PQuery surrogateQuery) {
        Preconditions.checkArgument(surrogateQuery != null, "Surrogate query FQN must not be null!");
        return addDynamicSurrogateQueryForFeature(feature, new SingletonInstanceProvider<PQuery>(surrogateQuery));
    }

    public IProvider<PQuery> addDynamicSurrogateQueryForFeature(IInputKey feature, IProvider<PQuery> surrogateQuery) {
        Preconditions.checkArgument(feature != null, "Feature must not be null!");
        Preconditions.checkArgument(surrogateQuery != null, "Surrogate query FQN must not be null!");
        return dynamicSurrogateQueryMap.put(feature, surrogateQuery);
    }

    public IProvider<PQuery> removeDynamicSurrogateQueryForFeature(IInputKey feature) {
        Preconditions.checkArgument(feature != null, "Feature must not be null!");
        return dynamicSurrogateQueryMap.remove(feature);
    }

    /**
     *
     * @param feature that may have surrogate query defined, null not allowed
     * @return true if the feature has a surrogate query defined
     * @throws IllegalArgumentException if feature is null
     */
    public boolean hasSurrogateQueryFQN(IInputKey feature) {
        Preconditions.checkArgument(feature != null, "Feature must not be null!");
        boolean surrogateExists = dynamicSurrogateQueryMap.containsKey(feature);
        if(!surrogateExists){
            surrogateExists = registeredSurrogateQueryMap.containsKey(feature);
        }
        return surrogateExists;
    }

    /**
     *
     * @param feature for which the surrogate query FQN should be returned
     * @return the surrogate query FQN defined for the feature
     * @throws IllegalArgumentException if feature is null
     * @throws NoSuchElementException if the feature has no surrogate query defined, use {@link #hasSurrogateQueryFQN} to check
     */
    public PQuery getSurrogateQuery(IInputKey feature) {
        Preconditions.checkArgument(feature != null, "Feature must not be null!");
        IProvider<PQuery> surrogate = dynamicSurrogateQueryMap.get(feature);
        if(surrogate == null) {
            surrogate = registeredSurrogateQueryMap.get(feature);
        }
        if(surrogate != null) {
            return surrogate.get();
        } else {
            throw new NoSuchElementException(String.format("Feature %s has no surrogate query defined! Use #hasSurrogateQueryFQN to check existence.", feature));
        }
    }

    /**
     * @return an unmodifiable set of features with registered surrogate queries
     */
    public Set<IInputKey> getRegisteredSurrogateQueries() {
        return Collections.unmodifiableSet(getRegisteredSurrogateQueriesInternal());
    }

    private Set<IInputKey> getRegisteredSurrogateQueriesInternal() {
        return registeredSurrogateQueryMap.keySet();
    }

    /**
     * @return an unmodifiable set of features with dynamically added surrogate queries
     */
    public Set<IInputKey> getDynamicSurrogateQueries() {
        return Collections.unmodifiableSet(getDynamicSurrogateQueriesInternal());
    }

    private Set<IInputKey> getDynamicSurrogateQueriesInternal() {
        return dynamicSurrogateQueryMap.keySet();
    }

    /**
     * @return an unmodifiable set that contains all features with surrogate queries.
     */
    public Set<IInputKey> getAllSurrogateQueries() {
        Set<IInputKey> results = new HashSet<>(getRegisteredSurrogateQueriesInternal());
        results.addAll(getDynamicSurrogateQueriesInternal());
        return results;
    }
}
