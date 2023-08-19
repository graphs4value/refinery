/*******************************************************************************
 * Copyright (c) 2004-2010 Gabor Bergmann and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.viatra.runtime;

import org.eclipse.core.runtime.Plugin;
import tools.refinery.viatra.runtime.internal.ExtensionBasedSurrogateQueryLoader;
import tools.refinery.viatra.runtime.internal.ExtensionBasedSystemDefaultBackendLoader;
import tools.refinery.viatra.runtime.registry.ExtensionBasedQuerySpecificationLoader;
import org.osgi.framework.BundleContext;

/**
 * The activator class controls the plug-in life cycle
 */
public class ViatraQueryRuntimePlugin extends Plugin {

    public static final String PLUGIN_ID = "tools.refinery.viatra.runtime";

    @Override
    public void start(BundleContext context) throws Exception {
        super.start(context);
        ExtensionBasedSurrogateQueryLoader.instance().loadKnownSurrogateQueriesIntoRegistry();
        ExtensionBasedQuerySpecificationLoader.getInstance().loadRegisteredQuerySpecificationsIntoRegistry();
        ExtensionBasedSystemDefaultBackendLoader.instance().loadKnownBackends();
    }

}
