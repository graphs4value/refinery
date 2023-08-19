/*******************************************************************************
 * Copyright (c) 2010-2018, Gabor Bergmann, IncQuery Labs Ltd.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.viatra.runtime.matchers.scopes.tables;

import tools.refinery.viatra.runtime.matchers.tuple.Tuple;
import tools.refinery.viatra.runtime.matchers.util.Direction;

/**
 * Modifies the contents of an {@link IIndexTable}.
 * <p>
 * <strong>EXPERIMENTAL</strong>. This class or interface has been added as
 * part of a work in progress. There is no guarantee that this API will
 * work or that it will remain the same.
 *
 * @since 2.0
 * @author Gabor Bergmann
 */
public interface ITableWriterGeneric {

    /**
     * Adds/removes a row to/from the table.
     * 
     * @param direction
     *            tells whether putting a row into the table or deleting TODO: store as multiset, return bool?
     */
    void write(Direction direction, Tuple row);

    /**
     * Intersection type for writers that are also tables
     */
    interface Table extends ITableWriterGeneric, IIndexTable {
    }

    /**
     * /dev/null implementation
     * 
     * @author Gabor Bergmann
     */
    static class Nop implements ITableWriterGeneric {
        @Override
        public void write(Direction direction, Tuple row) {
            // NO-OP
        }

    }
}
