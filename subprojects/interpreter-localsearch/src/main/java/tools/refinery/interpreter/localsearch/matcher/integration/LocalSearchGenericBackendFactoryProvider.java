/*******************************************************************************
 * Copyright (c) 2010-2018, Zoltan Ujhelyi, IncQuery Labs Ltd.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.interpreter.localsearch.matcher.integration;

import tools.refinery.interpreter.matchers.backend.IQueryBackendFactory;
import tools.refinery.interpreter.matchers.backend.IQueryBackendFactoryProvider;

/**
 * @since 2.0
 */
public class LocalSearchGenericBackendFactoryProvider implements IQueryBackendFactoryProvider {

    @Override
    public IQueryBackendFactory getFactory() {
        return LocalSearchGenericBackendFactory.INSTANCE;
    }

}
