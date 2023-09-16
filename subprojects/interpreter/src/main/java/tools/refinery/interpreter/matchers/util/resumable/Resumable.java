/*******************************************************************************
 * Copyright (c) 2010-2016, Tamas Szabo, Istvan Rath and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.interpreter.matchers.util.resumable;

/**
 * A resumable lazily folds its state towards higher timestamps. Folding shall be done in the increasing order of
 * timestamps, and it shall be interrupted after each step. The resumable can then be instructed to resume the folding,
 * one step at a time.
 *
 * @author Tamas Szabo
 * @since 2.4
 */
public interface Resumable<Timestamp extends Comparable<Timestamp>> {

    /**
     * Returns the smallest timestamp where lazy folding shall be resumed, or null if there is no more folding to do in this
     * resumable.
     */
    public Timestamp getResumableTimestamp();

}
