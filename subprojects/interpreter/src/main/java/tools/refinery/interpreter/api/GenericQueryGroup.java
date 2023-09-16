/*******************************************************************************
 * Copyright (c) 2010-2012, Mark Czotter, Istvan Rath and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.interpreter.api;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import tools.refinery.interpreter.api.impl.BaseQueryGroup;

/**
 * Generic implementation of {@link IQueryGroup}, covering an arbitrarily chosen set of patterns. Use the public
 * constructor or static GenericQueryGroup.of(...) methods to instantiate.
 *
 * @author Mark Czotter
 *
 */
public class GenericQueryGroup extends BaseQueryGroup {

    private final Set<IQuerySpecification<?>> patterns;

    /**
     * Creates a GenericQueryGroup object with a set of patterns.
     *
     * @param patterns
     */
    public GenericQueryGroup(Set<IQuerySpecification<?>> patterns) {
        this.patterns = patterns;
    }

    @Override
    public Set<IQuerySpecification<?>> getSpecifications() {
        return patterns;
    }

    /**
     * Creates a generic {@link IQueryGroup} instance from {@link IQuerySpecification} objects.
     *
     * @since 2.0
     */
    public static IQueryGroup of(Stream<IQuerySpecification<?>> querySpecifications) {
        return new GenericQueryGroup(querySpecifications.collect(Collectors.toSet()));
    }

    /**
     * Creates a generic {@link IQueryGroup} instance from {@link IQuerySpecification} objects.
     *
     * @param querySpecifications
     */
    public static IQueryGroup of(Set<IQuerySpecification<?>> querySpecifications) {
        return new GenericQueryGroup(querySpecifications);
    }

    /**
     * Creates a generic {@link IQueryGroup} instance from {@link IQuerySpecification} objects.
     *
     * @param querySpecifications
     */
    public static IQueryGroup of(IQuerySpecification<?>... querySpecifications) {
        return of(new HashSet<IQuerySpecification<?>>(Arrays.asList(querySpecifications)));
    }

    /**
     * Creates a generic {@link IQueryGroup} instance from other {@link IQueryGroup} objects (subgroups).
     *
     */
    public static IQueryGroup of(IQueryGroup... subGroups) {
        Set<IQuerySpecification<?>> patterns = new HashSet<IQuerySpecification<?>>();
        for (IQueryGroup group : subGroups) {
            patterns.addAll(group.getSpecifications());
        }
        return new GenericQueryGroup(patterns);
    }
}
