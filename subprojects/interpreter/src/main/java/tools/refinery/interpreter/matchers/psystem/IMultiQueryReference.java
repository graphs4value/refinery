/*******************************************************************************
 * Copyright (c) 2010-2022, Tamas Szabo, GitHub
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.interpreter.matchers.psystem;

import java.util.Collection;

import tools.refinery.interpreter.matchers.psystem.queries.PQuery;

/**
 * A {@link PConstraint} that implements this interface refers to a list of PQueries.
 *
 * @author Tamas Szabo
 * @since 2.8
 *
 */
public interface IMultiQueryReference {

    Collection<PQuery> getReferredQueries();

}
