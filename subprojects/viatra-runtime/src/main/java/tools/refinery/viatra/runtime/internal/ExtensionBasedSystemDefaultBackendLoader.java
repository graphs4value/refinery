/*******************************************************************************
 * Copyright (c) 2010-2018, Zoltan Ujhelyi, IncQuery Labs
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.viatra.runtime.internal;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;
import tools.refinery.viatra.runtime.api.ViatraQueryEngineOptions;
import tools.refinery.viatra.runtime.matchers.backend.IQueryBackendFactory;
import tools.refinery.viatra.runtime.matchers.backend.IQueryBackendFactoryProvider;
import tools.refinery.viatra.runtime.util.ViatraQueryLoggingUtil;

/**
 * @since 2.0
 */
public class ExtensionBasedSystemDefaultBackendLoader {

    private static final String EXTENSION_ID = "tools.refinery.viatra.runtime.querybackend";
    private static final ExtensionBasedSystemDefaultBackendLoader INSTANCE = new ExtensionBasedSystemDefaultBackendLoader();
    
    public static ExtensionBasedSystemDefaultBackendLoader instance() {
        return INSTANCE;
    }

    public void loadKnownBackends() {
        IQueryBackendFactory defaultBackend = null;
        IQueryBackendFactory defaultCachingBackend = null;
        IQueryBackendFactory defaultSearchBackend = null;
        final IConfigurationElement[] config = Platform.getExtensionRegistry().getConfigurationElementsFor(EXTENSION_ID);
        for (IConfigurationElement e : config) {
            try {
                IQueryBackendFactoryProvider provider = (IQueryBackendFactoryProvider) e
                        .createExecutableExtension("provider");
                if (provider.isSystemDefaultEngine()) {
                    defaultBackend = provider.getFactory();
                }
                if (provider.isSystemDefaultCachingBackend()) {
                    defaultCachingBackend = provider.getFactory();
                }
                if (provider.isSystemDefaultSearchBackend()) {
                    defaultSearchBackend = provider.getFactory();
                }
                
            } catch (CoreException ex) {
                // In case errors try to continue with the next one
                ViatraQueryLoggingUtil.getLogger(getClass()).error(
                        String.format("Error while initializing backend %s from plugin %s.",
                                e.getAttribute("backend"), e.getContributor().getName()), ex);
            }
        }
        ViatraQueryEngineOptions.setSystemDefaultBackends(defaultBackend, defaultCachingBackend, defaultSearchBackend);
    }
    
}
