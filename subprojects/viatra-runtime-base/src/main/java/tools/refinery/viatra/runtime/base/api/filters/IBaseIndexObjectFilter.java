/*******************************************************************************
 * Copyright (c) 2010-2014, Zoltan Ujhelyi, Istvan Rath and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.viatra.runtime.base.api.filters;

import org.eclipse.emf.common.notify.Notifier;

/**
 * 
 * Stores a collection of {@link Notifier} instances that need not to be indexed by VIATRA Base.
 * 
 * @author Zoltan Ujhelyi
 * 
 */
public interface IBaseIndexObjectFilter {

    /**
     * Decides whether the selected notifier is filtered.
     * 
     * @param notifier
     * @return true, if the notifier should not be indexed
     */
    boolean isFiltered(Notifier notifier);

}