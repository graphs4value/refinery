/*******************************************************************************
 * Copyright (c) 2010-2012, Mark Czotter, Istvan Rath and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.viatra.runtime.api.impl;

import tools.refinery.viatra.runtime.api.AdvancedViatraQueryEngine;
import tools.refinery.viatra.runtime.api.IQueryGroup;
import tools.refinery.viatra.runtime.api.ViatraQueryEngine;

/**
 * Base implementation of {@link IQueryGroup}.
 *
 * @author Mark Czotter
 *
 */
public abstract class BaseQueryGroup implements IQueryGroup {

    @Override
    public void prepare(ViatraQueryEngine engine) {
        prepare(AdvancedViatraQueryEngine.from(engine));
    }
    
    protected void prepare(AdvancedViatraQueryEngine engine) {
        engine.prepareGroup(this, null /* default options */);
    }
    

}
