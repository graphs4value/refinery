/*******************************************************************************
 * Copyright (c) 2004-2010 Gabor Bergmann and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package tools.refinery.interpreter.api.impl;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import tools.refinery.interpreter.exception.InterpreterException;
import tools.refinery.interpreter.matchers.psystem.annotations.PAnnotation;
import tools.refinery.interpreter.matchers.psystem.queries.PParameter;
import tools.refinery.interpreter.matchers.psystem.queries.PQuery;
import tools.refinery.interpreter.matchers.psystem.queries.PVisibility;
import tools.refinery.interpreter.matchers.psystem.queries.QueryInitializationException;
import tools.refinery.interpreter.api.IPatternMatch;
import tools.refinery.interpreter.api.IQuerySpecification;
import tools.refinery.interpreter.api.InterpreterEngine;
import tools.refinery.interpreter.api.InterpreterMatcher;

/**
 * Base implementation of IQuerySpecification.
 *
 * @author Gabor Bergmann
 *
 */
public abstract class BaseQuerySpecification<Matcher extends InterpreterMatcher<? extends IPatternMatch>> implements
        IQuerySpecification<Matcher> {

    /**
     * @since 1.6
     */
    protected static InterpreterException processInitializerError(ExceptionInInitializerError err) {
        Throwable cause1 = err.getCause();
        if (cause1 instanceof RuntimeException) {
            Throwable cause2 = ((RuntimeException) cause1).getCause();
            if (cause2 instanceof InterpreterException) {
                return (InterpreterException) cause2;
            } else if (cause2 instanceof QueryInitializationException) {
                return new InterpreterException((QueryInitializationException) cause2);
            }
        }
        throw err;
    }
    protected final PQuery wrappedPQuery;

    protected abstract Matcher instantiate(InterpreterEngine engine);

    /**
     * For backward compatibility of code generated with previous versions of viatra query, this method has a default
     * implementation returning null, indicating that a matcher can only be created using the old method, which ignores
     * the hints provided by the user.
     *
     * @since 1.4
     */
    @Override
    public Matcher instantiate() {
        return null;
    }


    /**
     * Instantiates query specification for the given internal query representation.
     */
    public BaseQuerySpecification(PQuery wrappedPQuery) {
        super();
        this.wrappedPQuery = wrappedPQuery;
        wrappedPQuery.publishedAs().add(this);
    }


    @Override
    public PQuery getInternalQueryRepresentation() {
        return wrappedPQuery;
    }

    @Override
    public Matcher getMatcher(InterpreterEngine engine) {
        ensureInitializedInternal();
        if (wrappedPQuery.getStatus() == PQuery.PQueryStatus.ERROR) {
            String errorMessages = wrappedPQuery.getPProblems().stream()
                    .map(input -> (input == null) ? "" : input.getShortMessage()).collect(Collectors.joining("\n"));
            throw new InterpreterException(String.format("Erroneous query specification: %s %n %s", getFullyQualifiedName(), errorMessages),
                    "Cannot initialize matchers on erroneous query specifications.");
        } else if (!engine.getScope().isCompatibleWithQueryScope(this.getPreferredScopeClass())) {
            throw new InterpreterException(
                    String.format(
                            "Scope class incompatibility: the query %s is formulated over query scopes of class %s, "
                                    + " thus the query engine formulated over scope %s of class %s cannot evaluate it.",
                            this.getFullyQualifiedName(), this.getPreferredScopeClass().getCanonicalName(),
                            engine.getScope(), engine.getScope().getClass().getCanonicalName()),
                    "Incompatible scope classes of engine and query.");
        }
        return instantiate(engine);
    }

    protected void ensureInitializedInternal() {
        wrappedPQuery.ensureInitialized();
    }

    // // DELEGATIONS

    @Override
    public List<PAnnotation> getAllAnnotations() {
        return wrappedPQuery.getAllAnnotations();
    }
    @Override
    public List<PAnnotation> getAnnotationsByName(String annotationName) {
        return wrappedPQuery.getAnnotationsByName(annotationName);
    }
    @Override
    public Optional<PAnnotation> getFirstAnnotationByName(String annotationName) {
        return wrappedPQuery.getFirstAnnotationByName(annotationName);
    }
    @Override
    public String getFullyQualifiedName() {
        return wrappedPQuery.getFullyQualifiedName();
    }
    @Override
    public List<String> getParameterNames() {
        return wrappedPQuery.getParameterNames();
    }
    @Override
    public List<PParameter> getParameters() {
        return wrappedPQuery.getParameters();
    }
    @Override
    public Integer getPositionOfParameter(String parameterName) {
        return wrappedPQuery.getPositionOfParameter(parameterName);
    }

    /**
     * @since 2.0
     */
    @Override
    public PVisibility getVisibility() {
        return wrappedPQuery.getVisibility();
    }

}
