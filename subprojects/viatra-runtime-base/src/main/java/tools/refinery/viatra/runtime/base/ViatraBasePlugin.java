/*******************************************************************************
 * Copyright (c) 2010-2012, Tamas Szabo, Gabor Bergmann, Istvan Rath and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package tools.refinery.viatra.runtime.base;

import org.eclipse.core.runtime.Plugin;
import tools.refinery.viatra.runtime.base.comprehension.WellbehavingDerivedFeatureRegistry;
import org.osgi.framework.BundleContext;

public class ViatraBasePlugin extends Plugin {

    // The shared instance
    private static ViatraBasePlugin plugin;

    public static final String PLUGIN_ID = "tools.refinery.viatra.runtime.base";
    public static final String WELLBEHAVING_DERIVED_FEATURE_EXTENSION_POINT_ID = "tools.refinery.viatra.runtime.base.wellbehaving.derived.features";

    @Override
    public void start(BundleContext context) throws Exception {
        super.start(context);
        plugin = this;
        WellbehavingDerivedFeatureRegistry.initRegistry();
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        plugin = null;
        super.stop(context);
    }

    /**
     * Returns the shared instance
     * 
     * @return the shared instance
     */
    public static ViatraBasePlugin getDefault() {
        return plugin;
    }

}
