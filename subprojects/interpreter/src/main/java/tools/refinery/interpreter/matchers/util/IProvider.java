/*******************************************************************************
 * Copyright (c) 2010-2015, Zoltan Ujhelyi, Istvan Rath and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.interpreter.matchers.util;

import java.util.function.Function;
import java.util.function.Supplier;

import tools.refinery.interpreter.matchers.psystem.queries.PQuery;

/**
 * A provider interface useful in various registry instances.
 *
 * @author Zoltan Ujhelyi
 *
 */
public interface IProvider<T> extends Supplier<T>{

    public final class ProvidedValueFunction implements Function<IProvider<PQuery>, PQuery> {
        @Override
        public PQuery apply(IProvider<PQuery> input) {
            return (input == null) ? null : input.get();
        }
    }
}
