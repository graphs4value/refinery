/*******************************************************************************
 * Copyright (c) 2010-2015, Marton Bur, Zoltan Ujhelyi, Akos Horvath, Istvan Rath and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.interpreter.matchers.psystem.rewriters;
import tools.refinery.interpreter.matchers.psystem.basicenumerables.PositivePatternCall;

/**
 * @author Marton Bur
 *
 */
public class DefaultFlattenCallPredicate implements IFlattenCallPredicate {

    @Override
    public boolean shouldFlatten(PositivePatternCall positivePatternCall) {
        return true;
    }

}
