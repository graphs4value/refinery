/*******************************************************************************
 * Copyright (c) 2010-2016, Abel Hegedus, IncQuery Labs Ltd.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.viatra.runtime.extensibility;

import tools.refinery.viatra.runtime.api.IQuerySpecification;
import tools.refinery.viatra.runtime.matchers.util.SingletonInstanceProvider;

/**
 * Provider implementation for storing an existing query specification instance.
 * 
 * @author Abel Hegedus
 * @since 1.3
 *
 */
public class SingletonQuerySpecificationProvider extends SingletonInstanceProvider<IQuerySpecification<?>>
        implements IQuerySpecificationProvider {

    /**
     * 
     * @param instance the instance to wrap
     */
    public SingletonQuerySpecificationProvider(IQuerySpecification<?> instance) {
        super(instance);
    }

    @Override
    public String getFullyQualifiedName() {
        return get().getFullyQualifiedName();
    }

    @Override
    public String getSourceProjectName() {
        return null;
    }

}
