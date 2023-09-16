/*******************************************************************************
 * Copyright (c) 2010-2015, Bergmann Gabor, Istvan Rath and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.interpreter.matchers.psystem.queries;

import java.util.List;
import java.util.Optional;

import tools.refinery.interpreter.matchers.psystem.annotations.PAnnotation;

/**
 * Represents header information (metainfo) about a query.
 * <p> To be implemented both by IQuerySpecifications intended for end users,
 * and the internal query representation {@link PQuery}.
 *
 *
 * @author Bergmann Gabor
 * @since 0.9
 */
public interface PQueryHeader {

    /**
     * Identifies the pattern for which matchers can be instantiated.
     */
    public String getFullyQualifiedName();

    /**
     * Return the list of parameter names
     *
     * @return a non-null, but possibly empty list of parameter names
     */
    public List<String> getParameterNames();

    /**
     * Returns a list of parameter descriptions
     *
     * @return a non-null, but possibly empty list of parameter descriptions
     */
    public List<PParameter> getParameters();

    /**
     * Returns the index of a named parameter
     *
     * @param parameterName
     * @return the index, or null of no such parameter is available
     */
    public Integer getPositionOfParameter(String parameterName);

    /**
     * Returns a parameter by name if exists
     * @since 2.1
     */
    default Optional<PParameter> getParameter(String parameterName) {
        return Optional.ofNullable(getPositionOfParameter(parameterName))
            .map(getParameters()::get);
    }

    /**
     * Returns the list of annotations specified for this query
     *
     * @return a non-null, but possibly empty list of annotations
     */
    public List<PAnnotation> getAllAnnotations();

    /**
     * Returns the list of annotations with a specified name
     *
     * @param annotationName
     * @return a non-null, but possibly empty list of annotations
     */
    public List<PAnnotation> getAnnotationsByName(String annotationName);

    /**
     * Returns the first annotation with a specified name
     *
     * @since 2.0
     */
    public Optional<PAnnotation> getFirstAnnotationByName(String annotationName);

    /**
     * Returns the visibility information about the query.
     * @since 2.0
     */
    public PVisibility getVisibility();

    /**
     * Returns the non-qualified name of the query. By default this means returning the qualified name after the last
     * '.' character.
     *
     * @since 2.0
     */
    public default String getSimpleName() {
        return PQueries.calculateSimpleName(getFullyQualifiedName());
    }

}
