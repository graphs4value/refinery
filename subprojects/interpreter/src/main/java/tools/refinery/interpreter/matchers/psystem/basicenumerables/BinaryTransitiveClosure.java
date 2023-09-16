/*******************************************************************************
 * Copyright (c) 2004-2010 Gabor Bergmann and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package tools.refinery.interpreter.matchers.psystem.basicenumerables;

import tools.refinery.interpreter.matchers.psystem.PBody;
import tools.refinery.interpreter.matchers.psystem.queries.PQuery;
import tools.refinery.interpreter.matchers.tuple.Tuple;

/**
 * For a binary base pattern, computes the irreflexive transitive closure (base)+
 *
 * @author Gabor Bergmann
 *
 */
public class BinaryTransitiveClosure extends AbstractTransitiveClosure {

    public BinaryTransitiveClosure(PBody pBody, Tuple variablesTuple,
            PQuery pattern) {
        super(pBody, variablesTuple, pattern);
    }

    @Override
    protected String keyToString() {
        return supplierKey.getFullyQualifiedName() + "+";
    }
}
