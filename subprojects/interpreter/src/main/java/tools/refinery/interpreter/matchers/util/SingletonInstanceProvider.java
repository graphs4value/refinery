/*******************************************************************************
 * Copyright (c) 2010-2015, Zoltan Ujhelyi, Istvan Rath and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.interpreter.matchers.util;

/**
 * A provider implementation that always returns the same object instance.
 * @author Zoltan Ujhelyi
 */
public class SingletonInstanceProvider<T> implements IProvider<T>{

    private T instance;

    public SingletonInstanceProvider(T instance) {
        Preconditions.checkArgument(instance != null, "Instance parameter must not be null.");
        this.instance = instance;
    }

    @Override
    public T get() {
        return instance;
    }

}
