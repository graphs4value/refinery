/*******************************************************************************
 * Copyright (c) 2010-2014, Zoltan Ujhelyi, Istvan Rath and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.viatra.runtime.base.api.filters;

import org.eclipse.emf.ecore.resource.Resource;

/**
 * Defines a filter for indexing resources
 * @author Zoltan Ujhelyi
 *
 */
public interface IBaseIndexResourceFilter {

    /**
     * Decides whether a selected resource needs to be indexed
     * @param resource
     * @return true, if the selected resource is filtered
     */
    boolean isResourceFiltered(Resource resource);

}