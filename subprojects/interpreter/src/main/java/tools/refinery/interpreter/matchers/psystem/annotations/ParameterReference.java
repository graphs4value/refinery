/*******************************************************************************
 * Copyright (c) 2010-2014, Zoltan Ujhelyi, Istvan Rath and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.interpreter.matchers.psystem.annotations;

/**
 * An annotation parameter referencing a query parameter by name. Does not check whether the parameter exists.
 *
 * @author Zoltan Ujhelyi
 *
 */
public class ParameterReference {

    final String name;

    public ParameterReference(String name) {
        super();
        this.name = name;
    }

    public String getName() {
        return name;
    }

}
