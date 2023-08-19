/*******************************************************************************
 * Copyright (c) 2010-2016, Grill Balázs, IncQuery Labs Ltd.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.viatra.runtime.localsearch.operations.extend.nobase;

import java.util.Collections;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.eclipse.emf.common.notify.Notifier;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import tools.refinery.viatra.runtime.emf.EMFScope;
import tools.refinery.viatra.runtime.localsearch.operations.extend.SingleValueExtendOperationExecutor;

/**
 * This abstract class provides a utility method for extenders to iterate over the given scope.
 * 
 * @author Grill Balázs
 * @noextend This class is not intended to be subclassed by clients.
 *
 */
abstract class AbstractIteratingExtendOperationExecutor<T> extends SingleValueExtendOperationExecutor<T> {

    private final EMFScope scope;
    
    public AbstractIteratingExtendOperationExecutor(int position, EMFScope scope) {
        super(position);
        this.scope = scope;
    }
    
    protected Stream<Notifier> getModelContents() {
        return scope.getScopeRoots().stream().map(input -> {
            if (input instanceof ResourceSet) {
                return ((ResourceSet) input).getAllContents();
            } else if (input instanceof Resource) {
                return ((Resource) input).getAllContents();
            } else if (input instanceof EObject) {
                return ((EObject) input).eAllContents();
            }
            return Collections.<Notifier> emptyIterator();
        }).map(i -> StreamSupport.stream(Spliterators.spliteratorUnknownSize(i, Spliterator.ORDERED), false))
                .flatMap(i -> i);
    }

}
