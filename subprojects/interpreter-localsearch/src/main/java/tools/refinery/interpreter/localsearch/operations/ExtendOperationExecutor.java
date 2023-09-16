/*******************************************************************************
 * Copyright (c) 2010-2013, Zoltan Ujhelyi, Akos Horvath, Istvan Rath and Daniel Varro
 * Copyright (c) 2023 The Refinery Authors <https://refinery.tools/>
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.interpreter.localsearch.operations;

import tools.refinery.interpreter.localsearch.MatchingFrame;
import tools.refinery.interpreter.localsearch.matcher.ISearchContext;

import java.util.Iterator;

/**
 * An operation that can be used to enumerate all possible values for a single position based on a constraint
 * @author Zoltan Ujhelyi, Akos Horvath
 * @noextend This class is not intended to be subclassed by clients.
 * @since 2.0
 */
public abstract class ExtendOperationExecutor<T> implements ISearchOperation.ISearchOperationExecutor {

    private Iterator<? extends T> it;

    /**
     * Returns an iterator with the possible options from the current state
     * @since 2.0
     */
    protected abstract Iterator<? extends T> getIterator(MatchingFrame frame, ISearchContext context);
    /**
     * Updates the frame with the next element of the iterator. Called during {@link #execute(MatchingFrame, ISearchContext)}.
     *
     * @return true if the update is successful or false otherwise; in case of false is returned, the next element should be taken from the iterator.
     * @since 2.0
     */
    protected abstract boolean fillInValue(T newValue, MatchingFrame frame, ISearchContext context);

    /**
     * Restores the frame to the state before {@link #fillInValue(Object, MatchingFrame, ISearchContext)}. Called during
     * {@link #onBacktrack(MatchingFrame, ISearchContext)}.
     *
     * @since 2.0
     */
    protected abstract void cleanup(MatchingFrame frame, ISearchContext context);

    @Override
    public void onInitialize(MatchingFrame frame, ISearchContext context) {
        it = getIterator(frame, context);
    }

    @Override
    public void onBacktrack(MatchingFrame frame, ISearchContext context) {
        it = null;

    }

	/**
	 * Fixed version that handles failed unification of variables correctly.
	 * @param frame The matching frame to extend.
	 * @param context The search context.
	 * @return {@code true} if an extension was found, {@code false} otherwise.
	 */
	@Override
	public boolean execute(MatchingFrame frame, ISearchContext context) {
		while (it.hasNext()) {
			var newValue = it.next();
			if (fillInValue(newValue, frame, context)) {
				return true;
			}
		}
		return false;
	}

}
