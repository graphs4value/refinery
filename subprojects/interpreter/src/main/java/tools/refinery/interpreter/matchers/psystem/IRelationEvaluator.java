/*******************************************************************************
 * Copyright (c) 2010-2022, Tamas Szabo, GitHub
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.interpreter.matchers.psystem;

import java.util.List;
import java.util.Set;

import tools.refinery.interpreter.matchers.tuple.Tuple;

/**
 * Implementations of this interface take an arbitrary number of input relations with their contents and compute the
 * tuples of a single output relation.
 *
 * @author Tamas Szabo
 * @since 2.8
 *
 */
public interface IRelationEvaluator {

    /**
     * A textual description of the evaluator. Used only for debug purposes, but must not be null.
     */
    String getShortDescription();

    /**
     * The relation evaluator code. For performance reasons, it is expected that the returned set is a mutable
     * collection, and the caller must be allowed to actually perform mutations!
     */
    Set<Tuple> evaluateRelation(List<Set<Tuple>> inputs) throws Exception;

    /**
     * For each input relation that this evaluator requires, this method returns the expected arities of the relations in order.
     */
    List<Integer> getInputArities();

    /**
     * Returns the arity of the output relation that this evaluator computes.
     */
    int getOutputArity();

}
