/*******************************************************************************
 * Copyright (c) 2010-2015, Zoltan Ujhelyi, Istvan Rath and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.viatra.runtime.extensibility;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import tools.refinery.viatra.runtime.api.IQuerySpecification;

/**
 * An extension factory to access PQuery instances from Query Specifications.
 * 
 * @author Zoltan Ujhelyi
 *
 */
public class PQueryExtensionFactory extends SingletonExtensionFactory {

    @Override
    public Object create() throws CoreException {
        final Object _spec = super.create();
        if (_spec instanceof IQuerySpecification<?>) {
            return ((IQuerySpecification<?>) _spec).getInternalQueryRepresentation();
        }
        throw new CoreException(new Status(IStatus.ERROR, getBundle().getSymbolicName(), "Cannot instantiate PQuery instance."));
    }

}
