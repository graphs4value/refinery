/*******************************************************************************
 * Copyright (c) 2010-2012, Mark Czotter, Istvan Rath and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.interpreter.api.impl;

import tools.refinery.interpreter.api.AdvancedInterpreterEngine;
import tools.refinery.interpreter.api.IQueryGroup;
import tools.refinery.interpreter.api.InterpreterEngine;

/**
 * Base implementation of {@link IQueryGroup}.
 *
 * @author Mark Czotter
 *
 */
public abstract class BaseQueryGroup implements IQueryGroup {

    @Override
    public void prepare(InterpreterEngine engine) {
        prepare(AdvancedInterpreterEngine.from(engine));
    }

    protected void prepare(AdvancedInterpreterEngine engine) {
        engine.prepareGroup(this, null /* default options */);
    }


}
