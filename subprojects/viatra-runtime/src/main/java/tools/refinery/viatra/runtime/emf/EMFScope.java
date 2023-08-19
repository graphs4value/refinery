/*******************************************************************************
 * Copyright (c) 2010-2014, Bergmann Gabor, Denes Harmath, Istvan Rath and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.viatra.runtime.emf;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;
import org.eclipse.emf.common.notify.Notifier;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import tools.refinery.viatra.runtime.api.AdvancedViatraQueryEngine;
import tools.refinery.viatra.runtime.api.ViatraQueryEngine;
import tools.refinery.viatra.runtime.api.scope.IEngineContext;
import tools.refinery.viatra.runtime.api.scope.IIndexingErrorListener;
import tools.refinery.viatra.runtime.api.scope.QueryScope;
import tools.refinery.viatra.runtime.base.api.BaseIndexOptions;
import tools.refinery.viatra.runtime.base.api.NavigationHelper;
import tools.refinery.viatra.runtime.exception.ViatraQueryException;

/**
 * An {@link QueryScope} consisting of EMF objects contained in multiple {@link ResourceSet}s, a single {@link ResourceSet}, {@link Resource} or a containment subtree below a given {@link EObject}.
 * 
 * <p> The scope is characterized by a root and some options (see {@link BaseIndexOptions}) such as dynamic EMF mode, subtree filtering etc.
 * <p>
 * The scope of pattern matching will be the given EMF model root(s) and below (see FAQ for more precise definition).
 * 
 * <p> Note on <i>cross-resource containment</i>: in case of {@link EObject} scopes, cross-resource containments will be considered part of the scope.
 * The same goes for {@link ResourceSet} scopes, provided that the resource of the contained element is successfully loaded into the resource set. 
 * In case of a {@link Resource} scope, containments pointing out from the resource will be excluded from the scope. 
 * Thus the boundaries of {@link EObject} scopes conform to the notion of EMF logical containment, while {@link Resource} and {@link ResourceSet} scopes adhere to persistence boundaries.
 * 
 * @author Bergmann Gabor
 *
 */
public class EMFScope extends QueryScope {
    
    private Set<? extends Notifier> scopeRoots;
    private BaseIndexOptions options;
    
    /**
     * Creates an EMF scope at the given root, with default options (recommended for most users).
     * @param scopeRoot the root of the EMF scope
     * @throws ViatraQueryRuntimeException- if scopeRoot is not an EMF ResourceSet, Resource or EObject
     */
    public EMFScope(Notifier scopeRoot) {
        this(Collections.singleton(scopeRoot), new BaseIndexOptions());
    }

    /**
     * Creates an EMF scope at the given root, with customizable options.
     * <p> Most users should consider {@link #EMFScope(Notifier)} instead.
     * @param scopeRoot the root of the EMF scope
     * @param options the base index building settings
     * @throws ViatraQueryRuntimeException if scopeRoot is not an EMF ResourceSet, Resource or EObject
     */
    public EMFScope(Notifier scopeRoot, BaseIndexOptions options) {
        this(Collections.singleton(scopeRoot), options);
    }

    /**
     * Creates an EMF scope at the given roots, with default options (recommended for most users).
     * @param scopeRoots the roots of the EMF scope, must be {@link ResourceSet}s
     * @throws ViatraQueryRuntimeException if not all scopeRoots are {@link ResourceSet}s
     */
    public EMFScope(Set<? extends ResourceSet> scopeRoots) {
        this(scopeRoots, new BaseIndexOptions());
    }

    /**
     * Creates an EMF scope at the given roots, with customizable options.
     * <p> Most users should consider {@link #EMFScope(Set)} instead.
     * @param scopeRoots the roots of the EMF scope, must be {@link ResourceSet}s
     * @param options the base index building settings
     * @throws ViatraQueryRuntimeException if not all scopeRoots are {@link ResourceSet}s
     */
    public EMFScope(Set<? extends Notifier> scopeRoots, BaseIndexOptions options) {
        super();
        if (scopeRoots.isEmpty()) {
            throw new IllegalArgumentException("No scope roots given");
        } else if (scopeRoots.size() == 1) {
            checkScopeRoots(scopeRoots, EObject.class::isInstance, Resource.class::isInstance, ResourceSet.class::isInstance);
        } else {
            checkScopeRoots(scopeRoots, ResourceSet.class::isInstance);
        }
        this.scopeRoots = new HashSet<>(scopeRoots);
        this.options = options.copy();
    }

    @SafeVarargs
    private final void checkScopeRoots(Set<? extends Notifier> scopeRoots, Predicate<Notifier>... predicates) {
        for (Notifier scopeRoot : scopeRoots) {
            // Creating compound predicate that checks the various branches of disjunction together
            Predicate<Notifier> compoundPredicate = Arrays.stream(predicates).collect(Collectors.reducing(a -> true, a -> a, (a, b) -> a.or(b)));
            if (!compoundPredicate.test(scopeRoot))
                throw new ViatraQueryException(ViatraQueryException.INVALID_EMFROOT
                        + (scopeRoot == null ? "(null)" : scopeRoot.getClass().getName()),
                        ViatraQueryException.INVALID_EMFROOT_SHORT);
        }
    }

    /**
     * @return the scope roots ({@link ResourceSet}s) containing the model
     */
    public Set<? extends Notifier> getScopeRoots() {
        return scopeRoots;
    }
    
    /**
     * @return the options
     */
    public BaseIndexOptions getOptions() {
        return options.copy();
    }
    
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((options == null) ? 0 : options.hashCode());
        result = prime * result
                + ((scopeRoots == null) ? 0 : scopeRoots.hashCode());
        return result;
    }
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (!(obj instanceof EMFScope))
            return false;
        EMFScope other = (EMFScope) obj;
        if (options == null) {
            if (other.options != null)
                return false;
        } else if (!options.equals(other.options))
            return false;
        if (scopeRoots == null) {
            if (other.scopeRoots != null)
                return false;
        } else if (!scopeRoots.equals(other.scopeRoots))
            return false;
        return true;
    }   
    

    @Override
    public String toString() {
        return String.format("EMFScope(%s):%s", options, scopeRoots.stream().map(this::scopeRootString).collect(Collectors.joining(",")));
    }
    
    private String scopeRootString(Notifier notifier) {
        if (notifier instanceof Resource) {
            Resource resource = (Resource) notifier;
            return String.format("%s(%s)", resource.getClass(), resource.getURI());
        } else if (notifier instanceof ResourceSet) {
            ResourceSet resourceSet = (ResourceSet) notifier;
            return resourceSet.getResources().stream()
                    .map(Resource::getURI)
                    .map(URI::toString)
                    .collect(Collectors.joining(", ", resourceSet.getClass() + "(", ")"));
        } else {
            return notifier.toString();
        }
    }

    @Override
    protected IEngineContext createEngineContext(ViatraQueryEngine engine, IIndexingErrorListener errorListener, Logger logger) {
        return new EMFEngineContext(this, engine, errorListener, logger);
    }

    /**
     * Provides access to the underlying EMF model index ({@link NavigationHelper}) from a VIATRA Query engine instantiated on an EMFScope
     * 
     * @param engine an already existing VIATRA Query engine instantiated on an EMFScope
     * @return the underlying EMF base index that indexes the contents of the EMF model
     * @throws ViatraQueryRuntimeException if base index initialization fails
     */
    public static NavigationHelper extractUnderlyingEMFIndex(ViatraQueryEngine engine) {
        final QueryScope scope = engine.getScope();
         if (scope instanceof EMFScope)
             return ((EMFBaseIndexWrapper)AdvancedViatraQueryEngine.from(engine).getBaseIndex()).getNavigationHelper();
         else throw new IllegalArgumentException("Cannot extract EMF base index from VIATRA Query engine instantiated on non-EMF scope " + scope);
    }
    
}
