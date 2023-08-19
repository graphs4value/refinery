/*******************************************************************************
 * Copyright (c) 2010-2012, Tamas Szabo, Gabor Bergmann, Istvan Rath and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package tools.refinery.viatra.runtime.base.api;

import org.eclipse.emf.ecore.EObject;
import tools.refinery.viatra.runtime.base.itc.igraph.ITcDataSource;

/**
 * The class can be used to compute the transitive closure of a given emf model, where the nodes will be the objects in
 * the model and the edges will be represented by the references between them. One must provide the set of references
 * that the helper should treat as edges when creating an instance with the factory: only the notifications about these
 * references will be handled.
 * 
 * @author Tamas Szabo
 * 
 */
public interface TransitiveClosureHelper extends ITcDataSource<EObject> {

}
