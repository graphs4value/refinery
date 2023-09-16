/*******************************************************************************
 * Copyright (c) 2010-2015, Bergmann Gabor, Istvan Rath and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.interpreter.matchers.backend;

import tools.refinery.interpreter.matchers.util.Preconditions;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Provides Refinery Interpreter with additional hints on how a query should be evaluated. The same hint can be provided
 * to multiple queries.
 *
 * <p> This class is immutable. Overriding options will create a new instance.
 *
 * <p>
 * Here be dragons: for advanced users only.
 *
 * @author Bergmann Gabor
 *
 */
public class QueryEvaluationHint {

    /**
     * @since 2.0
     *
     */
    public enum BackendRequirement {
        /**
         * The current hint does not specify any backend requirement
         */
        UNSPECIFIED,
        /**
         * The current hint specifies that the default search backend of the engine should be used
         */
        DEFAULT_SEARCH,
        /**
         * The current hint specifies that the default caching backend of the engine should be used
         */
        DEFAULT_CACHING,
        /**
         * The current hint specifies that a specific backend is to be used
         */
        SPECIFIC
    }

    final IQueryBackendFactory queryBackendFactory;
    final Map<QueryHintOption<?>, Object> backendHintSettings;
    final BackendRequirement requirement;

    /**
     * Specifies the suggested query backend requirements, and value settings for additional backend-specific options.
     *
     * <p>
     * The backend requirement type must not be {@link BackendRequirement#SPECIFIC} - for that case, use the constructor
     * {@link #QueryEvaluationHint(Map, IQueryBackendFactory)}.
     *
     * @param backendHintSettings
     *            if non-null, each entry in the map overrides backend-specific options regarding query evaluation
     *            (null-valued map entries permitted to erase hints); passing null means default options associated with
     *            the query
     * @param backendRequirementType
     *            defines the kind of backend requirement
     * @since 2.0
     */
    public QueryEvaluationHint(Map<QueryHintOption<?>, Object> backendHintSettings, BackendRequirement backendRequirementType) {
        super();
        Preconditions.checkArgument(backendRequirementType != null, "Specific requirement needs to be set");
        Preconditions.checkArgument(backendRequirementType != BackendRequirement.SPECIFIC, "Specific backend requirement needs providing a corresponding backend type");
        this.queryBackendFactory = null;
        this.requirement = backendRequirementType;
        this.backendHintSettings = (backendHintSettings == null)
                ? Collections.<QueryHintOption<?>, Object> emptyMap()
                : new HashMap<>(backendHintSettings);
    }

    /**
     * Specifies the suggested query backend, and value settings for additional backend-specific options. The first
     * parameter can be null; if the second parameter is null, it is expected that the other constructor is called
     * instead with a {@link BackendRequirement#UNSPECIFIED} parameter.
     *
     * @param backendHintSettings
     *            if non-null, each entry in the map overrides backend-specific options regarding query evaluation
     *            (null-valued map entries permitted to erase hints); passing null means default options associated with
     *            the query
     * @param queryBackendFactory
     *            overrides the query evaluator algorithm; passing null retains the default algorithm associated with
     *            the query
     * @since 1.5
     */
    public QueryEvaluationHint(
            Map<QueryHintOption<?>, Object> backendHintSettings,
            IQueryBackendFactory queryBackendFactory) {
        super();
        this.queryBackendFactory = queryBackendFactory;
        this.requirement = (queryBackendFactory == null) ? BackendRequirement.UNSPECIFIED : BackendRequirement.SPECIFIC;
        this.backendHintSettings = (backendHintSettings == null)
                ? Collections.<QueryHintOption<?>, Object> emptyMap()
                : new HashMap<>(backendHintSettings);
    }

    /**
     * Returns the backend requirement described by this hint. If a specific backend is required, that can be queried by {@link #getQueryBackendFactory()}.
     * @since 2.0
     */
    public BackendRequirement getQueryBackendRequirementType() {
        return requirement;
    }

    /**
     * A suggestion for choosing the query evaluator algorithm.
     *
     * <p>
     * Returns null iff {@link #getQueryBackendRequirementType()} does not return {@link BackendRequirement#SPECIFIC};
     * in such cases a corresponding default backend is selected inside the engine
     */
    public IQueryBackendFactory getQueryBackendFactory() {
        return queryBackendFactory;
    }

    /**
     * Each entry in the immutable map overrides backend-specific options regarding query evaluation.
     *
     * <p>The map is non-null, even if empty.
     * Null-valued map entries are also permitted to erase hints via {@link #overrideBy(QueryEvaluationHint)}.
     *
     * @since 1.5
     */
    public Map<QueryHintOption<?>, Object> getBackendHintSettings() {
        return backendHintSettings;
    }


    /**
     * Override values in this hint and return a consolidated instance.
     *
     * @since 1.4
     */
    public QueryEvaluationHint overrideBy(QueryEvaluationHint overridingHint){
        if (overridingHint == null)
            return this;

        BackendRequirement overriddenRequirement = this.getQueryBackendRequirementType();
        if (overridingHint.getQueryBackendRequirementType() != BackendRequirement.UNSPECIFIED) {
            overriddenRequirement = overridingHint.getQueryBackendRequirementType();
        }
        Map<QueryHintOption<?>, Object> hints = new HashMap<>(this.getBackendHintSettings());
        if (overridingHint.getBackendHintSettings() != null) {
            hints.putAll(overridingHint.getBackendHintSettings());
        }
        if (overriddenRequirement == BackendRequirement.SPECIFIC) {
            IQueryBackendFactory factory = this.getQueryBackendFactory();
            if (overridingHint.getQueryBackendFactory() != null) {
                factory = overridingHint.getQueryBackendFactory();
            }
            return new QueryEvaluationHint(hints, factory);
        } else {
            return new QueryEvaluationHint(hints, overriddenRequirement);
        }
    }

    /**
     * Returns whether the given hint option is overridden.
     * @since 1.5
     */
    public boolean isOptionOverridden(QueryHintOption<?> option) {
        return getBackendHintSettings().containsKey(option);
    }

    /**
     * Returns the value of the given hint option from the given hint collection, or null if not defined.
     * @since 1.5
     */
    @SuppressWarnings("unchecked")
    public <HintValue> HintValue getValueOrNull(QueryHintOption<HintValue> option) {
        return (HintValue) getBackendHintSettings().get(option);
    }

    /**
     * Returns the value of the given hint option from the given hint collection, or the default value if not defined.
     * Intended to be called by backends to find out the definitive value that should be considered.
     * @since 1.5
     */
    public <HintValue> HintValue getValueOrDefault(QueryHintOption<HintValue> option) {
        return option.getValueOrDefault(this);
    }

    @Override
    public int hashCode() {
        return Objects.hash(backendHintSettings, queryBackendFactory, requirement);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        QueryEvaluationHint other = (QueryEvaluationHint) obj;
        return Objects.equals(backendHintSettings, other.backendHintSettings)
               &&
               Objects.equals(queryBackendFactory, other.queryBackendFactory)
               &&
               Objects.equals(requirement, other.requirement)
        ;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        if (getQueryBackendFactory() != null)
            sb.append("backend: ").append(getQueryBackendFactory().getBackendClass().getSimpleName());
        if (! backendHintSettings.isEmpty()) {
            sb.append("hints: ");
            if(backendHintSettings instanceof AbstractMap){
                sb.append(backendHintSettings.toString());
            } else {
                // we have to iterate on the contents

                String joinedHintMap = backendHintSettings.entrySet().stream()
                        .map(setting -> setting.getKey() + "=" + setting.getValue()).collect(Collectors.joining(", "));
                sb.append('{').append(joinedHintMap).append('}');
            }
        }

        final String result = sb.toString();
        return result.isEmpty() ? "defaults" : result;
    }
}
