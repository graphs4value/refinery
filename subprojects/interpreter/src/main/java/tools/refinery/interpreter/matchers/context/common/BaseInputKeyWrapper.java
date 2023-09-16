/*******************************************************************************
 * Copyright (c) 2010-2015, Bergmann Gabor, Istvan Rath and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.interpreter.matchers.context.common;

import tools.refinery.interpreter.matchers.context.IInputKey;


/**
 * An input key that is identified by a single wrapped object and the class of the wrapper.
 * @author Bergmann Gabor
 *
 */
public abstract class BaseInputKeyWrapper<Wrapped> implements IInputKey {
    protected Wrapped wrappedKey;

    public BaseInputKeyWrapper(Wrapped wrappedKey) {
        super();
        this.wrappedKey = wrappedKey;
    }

    public Wrapped getWrappedKey() {
        return wrappedKey;
    }


    @Override
    public int hashCode() {
        return ((wrappedKey == null) ? 0 : wrappedKey.hashCode());
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (!(this.getClass().equals(obj.getClass())))
            return false;
        BaseInputKeyWrapper other = (BaseInputKeyWrapper) obj;
        if (wrappedKey == null) {
            if (other.wrappedKey != null)
                return false;
        } else if (!wrappedKey.equals(other.wrappedKey))
            return false;
        return true;
    }


}
