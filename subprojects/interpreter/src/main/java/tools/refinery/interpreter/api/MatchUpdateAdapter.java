/*******************************************************************************
 * Copyright (c) 2010-2012, Bergmann Gabor, Istvan Rath and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.interpreter.api;

import java.util.function.Consumer;

/**
 * A default implementation of {@link IMatchUpdateListener} that contains two match processors, one for appearance, one
 * for disappearance. Any of the two can be null; in this case, corresponding notifications will be ignored.
 *
 * <p>
 * Instantiate using either constructor.
 *
 * @author Bergmann Gabor
 *
 */
public class MatchUpdateAdapter<Match extends IPatternMatch> implements IMatchUpdateListener<Match> {

    Consumer<Match> appearCallback;
    Consumer<Match> disappearCallback;

    /**
     * Constructs an instance without any match processors registered yet.
     *
     * Use {@link #setAppearCallback(Consumer)} and {@link #setDisappearCallback(Consumer)} to specify
     * optional match processors for match appearance and disappearance, respectively.
     */
    public MatchUpdateAdapter() {
        super();
    }

    /**
     * Constructs an instance by specifying match processors.
     *
     * @param appearCallback
     *            a match processor that will be invoked on each new match that appears. If null, no callback will be
     *            executed on match appearance. See {@link Consumer} for details on how to implement.
     * @param disappearCallback
     *            a match processor that will be invoked on each existing match that disappears. If null, no callback
     *            will be executed on match disappearance. See {@link Consumer} for details on how to implement.
     * @since 2.0
     */
    public MatchUpdateAdapter(Consumer<Match> appearCallback, Consumer<Match> disappearCallback) {
        super();
        setAppearCallback(appearCallback);
        setDisappearCallback(disappearCallback);
    }

    /**
     * @return the match processor that will be invoked on each new match that appears. If null, no callback will be
     *         executed on match appearance.
     * @since 2.0
     */
    public Consumer<Match> getAppearCallback() {
        return appearCallback;
    }

    /**
     * @param appearCallback
     *            a match processor that will be invoked on each new match that appears. If null, no callback will be
     *            executed on match appearance. See {@link Consumer} for details on how to implement.
     * @since 2.0
     */
    public void setAppearCallback(Consumer<Match> appearCallback) {
        this.appearCallback = appearCallback;
    }

    /**
     * @return the match processor that will be invoked on each existing match that disappears. If null, no callback
     *         will be executed on match disappearance.
     * @since 2.0
     */
    public Consumer<Match> getDisappearCallback() {
        return disappearCallback;
    }

    /**
     * @param disappearCallback
     *            a match processor that will be invoked on each existing match that disappears. If null, no callback
     *            will be executed on match disappearance. See {@link Consumer} for details on how to implement.
     * @since 2.0
     */
    public void setDisappearCallback(Consumer<Match> disappearCallback) {
        this.disappearCallback = disappearCallback;
    }

    @Override
    public void notifyAppearance(Match match) {
        if (appearCallback != null)
            appearCallback.accept(match);
    }

    @Override
    public void notifyDisappearance(Match match) {
        if (disappearCallback != null)
            disappearCallback.accept(match);
    }

}
