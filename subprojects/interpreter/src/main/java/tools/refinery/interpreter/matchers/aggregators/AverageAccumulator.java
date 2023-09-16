/*******************************************************************************
 * Copyright (c) 2010-2018, Zoltan Ujhelyi, IncQuery Labs Ltd.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.interpreter.matchers.aggregators;

/**
 * @since 2.0
 */
class AverageAccumulator<Domain> {
    Domain value;
    long count;

    public AverageAccumulator(Domain value, long count) {
        super();
        this.value = value;
        this.count = count;
    }

}
