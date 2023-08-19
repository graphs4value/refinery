/*******************************************************************************
 * Copyright (c) 2010-2014, Zoltan Ujhelyi, Istvan Rath and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.viatra.runtime.extensibility;

import java.lang.reflect.Method;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExecutableExtension;
import org.eclipse.core.runtime.IExecutableExtensionFactory;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.osgi.framework.Bundle;

/**
 * Factory to register a static singleton instance in an extension point. 
 * 
 * @author Zoltan Ujhelyi
 * 
 */
public class SingletonExtensionFactory implements IExecutableExtension, IExecutableExtensionFactory {

    private String clazzName;
    private Bundle bundle;

    protected Bundle getBundle() {
        return bundle;
    }
    
    @Override
    public Object create() throws CoreException {
        try {
            final Class<?> clazz = bundle.loadClass(clazzName);
            Method method = clazz.getMethod("instance");
            return method.invoke(null);
        } catch (Exception e) {
            throw new CoreException(new Status(IStatus.ERROR, bundle.getSymbolicName(), "Error loading group "
                    + clazzName, e));
        }
    }

    @Override
    public void setInitializationData(IConfigurationElement config, String propertyName, Object data)
            throws CoreException {
        String id = config.getContributor().getName();
        bundle = Platform.getBundle(id);
        if (data instanceof String) {
            clazzName = (String) data;
        } else {
            throw new CoreException(new Status(IStatus.ERROR, bundle.getSymbolicName(),
                    "Unsupported extension initialization data: " + data));
        }
    }

}
