/*******************************************************************************
 * Copyright (c) 2010-2016, Abel Hegedus, IncQuery Labs Ltd.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.viatra.runtime.registry;

/**
 * This interface can be used to ask the registry to construct specific view instances. The factory is responsible for
 * instantiating the view, but the registry is responsible for establishing the connection with the internal data store
 * and to fill up the view with entries. Instances of the factory are intended to be passed to
 * {@link IQuerySpecificationRegistry#createView(IRegistryViewFactory)} and only the view instance returned by that
 * method can be considered initialized.
 * 
 * @author Abel Hegedus
 * @since 1.3
 *
 */
public interface IRegistryViewFactory {

    /**
     * Instantiate a new view object and store the reference to the registry.
     * This method should only be called by an {@link IQuerySpecificationRegistry}.
     * 
     * @param registry that will be connected to the view
     * @return the new instance of the view
     * @noreference This method is not intended to be referenced by clients.
     */
    IRegistryView createView(IQuerySpecificationRegistry registry);

}
