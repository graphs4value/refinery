/*******************************************************************************
 * Copyright (c) 2010-2012, Mark Czotter, Istvan Rath and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.interpreter.api.impl;

import java.util.HashSet;
import java.util.Set;

import tools.refinery.interpreter.api.IQuerySpecification;

/**
 * @author Mark Czotter
 *
 */
public abstract class BaseGeneratedPatternGroup extends BaseQueryGroup {

    @Override
    public Set<IQuerySpecification<?>> getSpecifications() {
        return querySpecifications;
    }

    /**
     * Returns {@link IQuerySpecification} objects for handling them as a group. To be filled by constructors of subclasses.
     */
    protected Set<IQuerySpecification<?>> querySpecifications = new HashSet<IQuerySpecification<?>>();
}
