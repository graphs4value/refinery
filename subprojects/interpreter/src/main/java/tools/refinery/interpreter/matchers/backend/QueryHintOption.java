/*******************************************************************************
 * Copyright (c) 2010-2016, Gabor Bergmann, IncQueryLabs Ltd.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.interpreter.matchers.backend;

import java.util.Map;
import java.util.Objects;

/**
 * Each instance of this class corresponds to a given hint option.
 *
 * It is recommended to expose options to clients (and query backends) as public static fields.
 *
 * @author Gabor Bergmann
 * @since 1.5
 */
public class QueryHintOption<HintValue> {

    private String optionQualifiedName;
    private HintValue defaultValue;

    /**
     * Instantiates an option object with the given name and default value.
     */
    public QueryHintOption(String optionQualifiedName, HintValue defaultValue) {
        this.optionQualifiedName = optionQualifiedName;
        this.defaultValue = defaultValue;
    }

    /**
     * This is the recommended constructor for hint options defined as static fields within an enclosing class.
     * Combines the qualified name of the hint from the (qualified) name of the enclosing class and a local name (unique within that class).
     */
    public <T extends HintValue> QueryHintOption(Class<?> optionNamespace, String optionLocalName, T defaultValue) {
        this(String.format("%s@%s", optionLocalName, optionNamespace.getName()), defaultValue);
    }

    /**
     * Returns the qualified name, a unique string identifier of the option.
     */
    public String getQualifiedName() {
        return optionQualifiedName;
    }

    /**
     * Returns the default value of this hint option, which is to be used by query backends in the case no overriding value is assigned.
     */
    public HintValue getDefaultValue() {
        return defaultValue;
    }

    /**
     * Returns the value of this hint option from the given hint collection, or the default value if not defined.
     * Intended to be called by backends to find out the definitive value that should be considered.
     */
    @SuppressWarnings("unchecked")
    public HintValue getValueOrDefault(QueryEvaluationHint hints) {
        Object value = hints.getValueOrNull(this);
        if (value == null)
            return getDefaultValue();
        else {
            return (HintValue) value;
        }
    }


    /**
     * Returns the value of this hint option from the given hint collection, or null if not defined.
     */
    public HintValue getValueOrNull(QueryEvaluationHint hints) {
        return hints.getValueOrNull(this);
    }

    /**
     * Returns whether this hint option is defined in the given hint collection.
     */
    public boolean isOverriddenIn(QueryEvaluationHint hints) {
        return hints.isOptionOverridden(this);
    }

    /**
     * Puts a value of this hint option into an option-to-value map.
     *
     * <p> This method is offered in lieu of a builder API.
     * Use this method on any number of hint options in order to populate an option-value map.
     * Then instantiate the immutable {@link QueryEvaluationHint} using the map.
     *
     * @see #insertValueIfNondefault(Map, Object)
     * @return the hint value that was previously present in the map under this hint option, carrying over the semantics of {@link Map#put(Object, Object)}.
     */
    @SuppressWarnings("unchecked")
    public HintValue insertOverridingValue(Map<QueryHintOption<?>, Object> hints, HintValue overridingValue) {
        return (HintValue) hints.put(this, overridingValue);
    }

    /**
     * Puts a value of this hint option into an option-to-value map, if the given value differs from the default value of the option.
     * If the default value is provided instead, then the map is not updated.
     *
     * <p> This method is offered in lieu of a builder API.
     * Use this method on any number of hint options in order to populate an option-value map.
     * Then instantiate the immutable {@link QueryEvaluationHint} using the map.
     *
     * @see #insertOverridingValue(Map, Object)
     * @since 2.0
     */
    public void insertValueIfNondefault(Map<QueryHintOption<?>, Object> hints, HintValue overridingValue) {
        if (!Objects.equals(defaultValue, overridingValue))
            hints.put(this, overridingValue);
    }

    @Override
    public String toString() {
        return optionQualifiedName;
    }

}
