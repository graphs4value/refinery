/*******************************************************************************
 * Copyright (c) 2004-2010 Gabor Bergmann and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package tools.refinery.interpreter.matchers.psystem;

import tools.refinery.interpreter.matchers.tuple.Tuple;

/**
 * @author Gabor Bergmann
 *
 */
public abstract class KeyedEnumerablePConstraint<KeyType> extends EnumerablePConstraint {

    protected KeyType supplierKey;

    public KeyedEnumerablePConstraint(PBody pBody, Tuple variablesTuple,
            KeyType supplierKey) {
        super(pBody, variablesTuple);
        this.supplierKey = supplierKey;
    }

    @Override
    protected String toStringRestRest() {
        return supplierKey == null ? "$any(null)" : keyToString();
    }

    protected abstract String keyToString();

    public KeyType getSupplierKey() {
        return supplierKey;
    }

}
