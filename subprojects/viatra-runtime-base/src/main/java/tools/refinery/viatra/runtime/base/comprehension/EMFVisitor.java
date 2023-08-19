/*******************************************************************************
 * Copyright (c) 2004-2010 Gabor Bergmann and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package tools.refinery.viatra.runtime.base.comprehension;

import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.resource.Resource;

/**
 * Use EMFModelComprehension to visit an EMF model.
 * 
 * @author Bergmann Gábor
 * 
 */
// FIXME:
// - handle boundary of active emfRoot subtree
// - more efficient traversal
public class EMFVisitor {

    boolean preOrder;

    public EMFVisitor(boolean preOrder) {
        super();
        this.preOrder = preOrder;
    }

    /**
     * @param resource
     * @param element
     */
    public void visitTopElementInResource(Resource resource, EObject element) {
    }

    /**
     * @param resource
     */
    public void visitResource(Resource resource) {
    }

    /**
     * @param source
     */
    public void visitElement(EObject source) {
    }

    /**
     * @param source
     * @param feature
     * @param target
     */
    public void visitNonContainmentReference(EObject source, EReference feature, EObject target) {
    }

    /**
     * @param source
     * @param feature
     * @param target
     */
    public void visitInternalContainment(EObject source, EReference feature, EObject target) {
    }

    /**
     * @param source
     * @param feature
     * @param target
     */
    public void visitAttribute(EObject source, EAttribute feature, Object target) {
    }

    /**
     * Returns true if the given feature should not be traversed (interesting esp. if multi-valued)
     */
    public boolean pruneFeature(EStructuralFeature feature) {
        return false;
    }

    /**
     * Returns true if the contents of an object should be pruned (and not explored by the visitor)
     */
    public boolean pruneSubtrees(EObject source) {
        return false;
    }

    /**
     * Returns true if the contents of a resource should be pruned (and not explored by the visitor)
     */
    public boolean pruneSubtrees(Resource source) {
        return false;
    }

    /**
     * An opportunity for the visitor to indicate that the containment link is considered in a transient state, and the
     * model comprehension should avoid following it.
     * 
     * A containment is in a transient state from the point of view of the visitor if it connects a subtree that is
     * being inserted <em>during</em> a full-model traversal, and a separate notification handler will deal with it
     * later.
     */
    public boolean avoidTransientContainmentLink(EObject source, EReference reference, EObject targetObject) {
        return false;
    }

    /**
     * @return if objects should be visited before their outgoing edges
     */
    public boolean preOrder() {
        return preOrder;
    }

    /**
     * Called after visiting the reference, if the target is a proxy.
     * 
     * @param position
     *            optional: known position in multivalued collection (for more efficient proxy resolution)
     */
    public void visitProxyReference(EObject source, EReference reference, EObject targetObject, Integer position) {
    }

    /**
     * Whether the given reference of the given object should be resolved when it is a proxy 
     */
    public boolean attemptProxyResolutions(EObject source, EReference feature) {
        return true;
    }

    /**
     * @return true if traversing visitors shall descend along cross-resource containments
     * (this only makes sense for traversing visitors on an object scope)
     * 
     * @since 1.7
     */
    public boolean descendAlongCrossResourceContainments() {
        return false;
    }

}