/*******************************************************************************
 * Copyright (c) 2010-2013, Abel Hegedus, Istvan Rath and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.interpreter.api.scope;

/**
 * Listener interface for change notifications from the Refinery Base index.
 *
 * @author Abel Hegedus
 * @since 0.9
 *
 */
public interface InterpreterBaseIndexChangeListener {

    /**
     * NOTE: it is possible that this method is called only ONCE! Consider returning a constant value that is set in the constructor.
     *
     * @return true, if the listener should be notified only after index changes, false if notification is needed after each model change
     */
    public boolean onlyOnIndexChange();

    /**
     * Called after a model change is handled by the Refinery Interpreter base index and if <code>indexChanged ==
	 * onlyOnIndexChange()</code>.
     *
     * @param indexChanged true, if the model change also affected the contents of the base index
     */
    public void notifyChanged(boolean indexChanged);

}
