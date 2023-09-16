/*******************************************************************************
 * Copyright (c) 2010-2013, Zoltan Ujhelyi, Istvan Rath and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.interpreter.matchers.tuple;

import java.util.Map;

import tools.refinery.interpreter.matchers.psystem.IValueProvider;

/**
 * @author Zoltan Ujhelyi
 * @since 1.7
 */
public class TupleValueProvider implements IValueProvider {

    final ITuple tuple;
    final Map<String, Integer> indexMapping;

    /**
     * Wraps a tuple with an index mapping
     * @param tuple
     * @param indexMapping
     */
    public TupleValueProvider(ITuple tuple, Map<String, Integer> indexMapping) {
        super();
        this.tuple = tuple;
        this.indexMapping = indexMapping;
    }

    @Override
    public Object getValue(String variableName) {
        Integer index = indexMapping.get(variableName);
        if (index == null) {
            throw new IllegalArgumentException(String.format("Variable %s is not present in mapping.", variableName));
        }
        Object value = tuple.get(index);
        if (value == null) {
            throw new IllegalArgumentException(String.format("Variable %s is not found using index %d.", variableName, index));
        }
        return value;
    }

}
