/*******************************************************************************
 * Copyright (c) 2010-2014, Zoltan Ujhelyi, Istvan Rath and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.interpreter.matchers.psystem;

import java.util.Collections;
import java.util.List;

import tools.refinery.interpreter.matchers.psystem.queries.PQuery;

/**
 * A {@link PConstraint} that implements this interface refers to a {@link PQuery}.
 *
 * @author Zoltan Ujhelyi
 *
 */
public interface IQueryReference extends IMultiQueryReference {

    PQuery getReferredQuery();

    /**
     * @since 2.8
     */
    @Override
    default List<PQuery> getReferredQueries() {
        return Collections.singletonList(getReferredQuery());
    }
}
