/*******************************************************************************
 * Copyright (c) 2010-2014, Bergmann Gabor, Istvan Rath and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.interpreter.api.scope;

import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.Callable;

/**
 * Represents the index maintained on the model.
 * @author Bergmann Gabor
 * @since 0.9
 *
 */
public interface IBaseIndex {
    // TODO lightweightObserver?
    // TODO ViatraBaseIndexChangeListener?

    /**
     * The given callback will be executed, and all model traversals and index registrations will be delayed until the
     * execution is done. If there are any outstanding feature, class or datatype registrations, a single coalesced model
     * traversal will initialize the caches and deliver the notifications.
     *
     * @param callable
     */
    public <V> V coalesceTraversals(Callable<V> callable) throws InvocationTargetException;

    /**
     * Adds a coarse-grained listener that will be invoked after the NavigationHelper index or the underlying model is changed. Can be used
     * e.g. to check model contents. Not intended for general use.
     *
     * <p/> See {@link #removeBaseIndexChangeListener(InterpreterBaseIndexChangeListener)}
     * @param listener
     */
    public void addBaseIndexChangeListener(InterpreterBaseIndexChangeListener listener);

    /**
     * Removes a registered listener.
     *
     * <p/> See {@link #addBaseIndexChangeListener(InterpreterBaseIndexChangeListener)}
     *
     * @param listener
     */
    public void removeBaseIndexChangeListener(InterpreterBaseIndexChangeListener listener);

    /**
     * Updates the value of indexed derived features that are not well-behaving.
     */
    void resampleDerivedFeatures();

    /**
     * Adds a listener for internal errors in the index. A listener can only be added once.
     * @param listener
     * @returns true if the listener was not already added
     * @since 0.8.0
     */
    boolean addIndexingErrorListener(IIndexingErrorListener listener);
    /**
     * Removes a listener for internal errors in the index
     * @param listener
     * @returns true if the listener was successfully removed (e.g. it did exist)
     * @since 0.8.0
     */
    boolean removeIndexingErrorListener(IIndexingErrorListener listener);

    /**
     * Register a lightweight observer that is notified if any edge starting at the given Object changes.
     *
     * @param observer the listener instance
     * @param observedObject the observed instance object
     * @return false if no observer can be registered for the given instance (e.g. it is a primitive),
     * 	or observer was already registered (call has no effect)
     */
    public boolean addInstanceObserver(IInstanceObserver observer, Object observedObject);

    /**
     * Unregisters a lightweight observer for the given Object.
     *
     * @param observer the listener instance
     * @param observedObject the observed instance object
     * @return false if no observer can be registered for the given instance (e.g. it is a primitive),
     * 	or no observer was registered previously (call has no effect)
     */
    public boolean removeInstanceObserver(IInstanceObserver observer, Object observedObject);

}
