/*******************************************************************************
 * Copyright (c) 2010-2016, Abel Hegedus, IncQuery Labs Ltd.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.viatra.runtime.extensibility;

import java.util.Set;
import java.util.stream.Collectors;

import tools.refinery.viatra.runtime.api.IQueryGroup;
import tools.refinery.viatra.runtime.api.IQuerySpecification;
import tools.refinery.viatra.runtime.matchers.util.SingletonInstanceProvider;

/**
 * Provider implementation for storing an existing query group instance.
 * 
 * @author Abel Hegedus
 * @since 1.3
 *
 */
public class SingletonQueryGroupProvider extends SingletonInstanceProvider<IQueryGroup> implements IQueryGroupProvider {

    /**
     * @param instance the instance to wrap
     */
    public SingletonQueryGroupProvider(IQueryGroup instance) {
        super(instance);
    }

    @Override
    public Set<String> getQuerySpecificationFQNs() {
        return get().getSpecifications().stream().map(IQuerySpecification::getFullyQualifiedName)
                .collect(Collectors.toSet());
    }

    @Override
    public Set<IQuerySpecificationProvider> getQuerySpecificationProviders() {
        return get().getSpecifications().stream().map(SingletonQuerySpecificationProvider::new)
                .collect(Collectors.toSet());
    }

}
