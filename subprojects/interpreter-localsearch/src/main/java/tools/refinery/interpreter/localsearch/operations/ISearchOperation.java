/*******************************************************************************
 * Copyright (c) 2010-2013, Zoltan Ujhelyi, Akos Horvath, Istvan Rath and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.interpreter.localsearch.operations;

import java.util.List;
import java.util.function.Function;

import tools.refinery.interpreter.localsearch.MatchingFrame;
import tools.refinery.interpreter.localsearch.matcher.ISearchContext;
import tools.refinery.interpreter.matchers.InterpreterRuntimeException;

/**
 * Represents a search operation executable by the LS engine. It is expected that an operation can be shared among
 * multiple LS matchers, but the created executors are not.
 *
 * @author Zoltan Ujhelyi
 *
 */
public interface ISearchOperation {

    /**
     * Initializes a new operation executor for the given operation. Repeated calls must return different executor
     * instances.
     *
     * @since 2.0
     */
    public ISearchOperationExecutor createExecutor();

    /**
     *
     * @since 2.0
     *
     */
    public interface ISearchOperationExecutor {

        /**
         * Returns the stateless operation this executor was initialized from
         */
        ISearchOperation getOperation();

        /**
         * During the execution of the corresponding plan, the onInitialize callback is evaluated before the execution of
         * the operation may begin. Operations may use this method to initialize its internal data structures.
         *
         * @throws InterpreterRuntimeException
         */
        void onInitialize(MatchingFrame frame, ISearchContext context);

        /**
         * After the execution of the operation failed and {@link #execute(MatchingFrame, ISearchContext)} returns false, the onBacktrack
         * callback is evaluated. Operations may use this method to clean up any temporary structures, and make the
         * operation ready for a new execution.
         *
         * @throws InterpreterRuntimeException
         */
        void onBacktrack(MatchingFrame frame, ISearchContext context);

        /**
         *
         * @param frame
         * @param context
         * @return true if successful, or false if backtracking needed
         * @throws InterpreterRuntimeException
         */
        boolean execute(MatchingFrame frame, ISearchContext context);
    }
    /**
     *
     * @return the ordered list of the variable numbers that are affected by the search operation
     */
    List<Integer> getVariablePositions();

    /**
     * Creates a string representation of the search operation by replacing the variable numbers according to the
     * parameter function. It is expected that the provided function does return a non-null value for each variable
     * index that is returned by {@link #getVariablePositions()}; otherwise a {@link NullPointerException} will be
     * thrown during the calculation of the string.
     *
     * @since 2.0
     */
    String toString(Function<Integer, String> variableMapping);
}
