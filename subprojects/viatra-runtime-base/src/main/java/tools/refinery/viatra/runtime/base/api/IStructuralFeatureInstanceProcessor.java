/*******************************************************************************
 * Copyright (c) 2010-2017, Gabor Bergmann, IncQueryLabs Ltd.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.viatra.runtime.base.api;

import org.eclipse.emf.ecore.EObject;

/**
 * @author Gabor Bergmann
 * @since 1.7
 */
public interface IStructuralFeatureInstanceProcessor {
    void process(EObject source, Object target);
}
