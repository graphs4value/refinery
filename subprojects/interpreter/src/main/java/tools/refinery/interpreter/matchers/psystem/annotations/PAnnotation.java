/*******************************************************************************
 * Copyright (c) 2010-2014, Zoltan Ujhelyi, Istvan Rath and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.interpreter.matchers.psystem.annotations;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;

import org.eclipse.collections.api.multimap.MutableMultimap;
import org.eclipse.collections.impl.multimap.list.FastListMultimap;

/**
 * A container describing query annotations
 * @author Zoltan Ujhelyi
 *
 */
public class PAnnotation {

    private final String name;
    private MutableMultimap<String, Object> attributes = FastListMultimap.newMultimap();

    public PAnnotation(String name) {
        this.name = name;

    }

    /**
     * Adds an attribute to the annotation
     * @param attributeName
     * @param value
     */
    public void addAttribute(String attributeName, Object value) {
        attributes.put(attributeName, value);
    }

    /**
     * Return the name of the annotation
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the value of the first occurrence of an attribute
     * @param attributeName
     * @return the attribute value, or null, if attribute is not available
     * @since 2.0
     */
    public Optional<Object> getFirstValue(String attributeName) {
        return getAllValues(attributeName).stream().findFirst();
    }

    /**
     * Returns the value of the first occurrence of an attribute
     * @param attributeName
     * @return the attribute value, or null, if attribute is not available
     * @since 2.0
     */
    public <T> Optional<T> getFirstValue(String attributeName, Class<T> clazz) {
        return getAllValues(attributeName).stream().filter(clazz::isInstance).map(clazz::cast).findFirst();
    }

    /**
     * Returns all values of a selected attribute
     * @param attributeName
     * @return a non-null, but possibly empty list of attributes
     */
    public List<Object> getAllValues(String attributeName) {
        return attributes.get(attributeName).toList();
    }

    /**
     * Executes a consumer over all attributes. A selected attribute name (key) can appear (and thus consumed) multiple times.
     * @since 2.0
     */
    public void forEachValue(BiConsumer<String, Object> consumer) {
        attributes.forEachKeyValue(consumer::accept);
    }

    /**
     * Returns a set of all attribute names used in this annotation
     * @since 2.1
     */
    public Set<String> getAllAttributeNames() {
        return attributes.keySet().toSet();
    }
}
