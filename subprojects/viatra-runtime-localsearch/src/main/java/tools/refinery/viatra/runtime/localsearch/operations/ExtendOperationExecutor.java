/*******************************************************************************
 * Copyright (c) 2010-2013, Zoltan Ujhelyi, Akos Horvath, Istvan Rath and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.viatra.runtime.localsearch.operations;

import java.util.Iterator;

import tools.refinery.viatra.runtime.localsearch.MatchingFrame;
import tools.refinery.viatra.runtime.localsearch.matcher.ISearchContext;
import tools.refinery.viatra.runtime.localsearch.operations.ISearchOperation.ISearchOperationExecutor;

/**
 * An operation that can be used to enumerate all possible values for a single position based on a constraint
 * @author Zoltan Ujhelyi, Akos Horvath
 * @noextend This class is not intended to be subclassed by clients.
 * @since 2.0
 */
public abstract class ExtendOperationExecutor<T> implements ISearchOperationExecutor {

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

    @Override
    public boolean execute(MatchingFrame frame, ISearchContext context) {
        if (it.hasNext()){
            T newValue = it.next();
            while(!fillInValue(newValue, frame, context) && it.hasNext()){
                newValue = it.next();
            }
            return true;
        } else {
            return false;
        }
    }
    
}
