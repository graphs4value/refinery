/*******************************************************************************
 * Copyright (c) 2004-2010 Gabor Bergmann and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package tools.refinery.interpreter.matchers.psystem;

import java.util.Set;

import tools.refinery.interpreter.matchers.context.IQueryMetaContext;

/**
 * @author Gabor Bergmann
 *
 */
public interface ITypeInfoProviderConstraint extends PConstraint {

    /**
     * Returns type information implied by this constraint.
     *
     */
    public Set<TypeJudgement> getImpliedJudgements(IQueryMetaContext context);

}
