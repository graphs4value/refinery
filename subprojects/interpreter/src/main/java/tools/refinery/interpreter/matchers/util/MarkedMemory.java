/*******************************************************************************
 * Copyright (c) 2010-2018, Gabor Bergmann, IncQueryLabs Ltd.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.interpreter.matchers.util;

/**
 * Internal marker type, must only be instantiated inside implementors of IMultiLookupImpl
 * @noimplement This interface is not intended to be implemented by clients.
 * @since 2.0
 */
public interface MarkedMemory<Value> extends IMemory<Value> {

    static interface MarkedSet<Value> extends MarkedMemory<Value> {}
    static interface MarkedMultiset<Value> extends MarkedMemory<Value> {}
    static interface MarkedDeltaBag<Value> extends MarkedMemory<Value> {}
}
