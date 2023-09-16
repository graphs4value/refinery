/*******************************************************************************
 * Copyright (c) 2010-2013, Abel Hegedus, Istvan Rath and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.interpreter.internal.engine;

import org.apache.log4j.Logger;
import tools.refinery.interpreter.api.*;
import tools.refinery.interpreter.api.InterpreterModelUpdateListener.ChangeLevel;
import tools.refinery.interpreter.api.scope.InterpreterBaseIndexChangeListener;
import tools.refinery.interpreter.exception.InterpreterException;
import tools.refinery.interpreter.matchers.util.CollectionsFactory;

import java.util.*;
import java.util.Map.Entry;

public final class ModelUpdateProvider extends ListenerContainer<InterpreterModelUpdateListener> {

    private final AdvancedInterpreterEngine queryEngine;
    private ChangeLevel currentChange = ChangeLevel.NO_CHANGE;
    private ChangeLevel maxLevel = ChangeLevel.NO_CHANGE;
    private final Map<ChangeLevel, Collection<InterpreterModelUpdateListener>> listenerMap;
    private final Logger logger;

    public ModelUpdateProvider(AdvancedInterpreterEngine queryEngine, Logger logger) {
        super();
        this.queryEngine = queryEngine;
        this.logger = logger;
        listenerMap = new EnumMap<>(ChangeLevel.class);
    }

    @Override
    protected void listenerAdded(InterpreterModelUpdateListener listener) {
        // check ChangeLevel
        // create callback for given level if required
        if(listenerMap.isEmpty()) {
            try {
                this.queryEngine.getBaseIndex().addBaseIndexChangeListener(indexListener);
                // add listener to new matchers (use lifecycle listener)
                this.queryEngine.addLifecycleListener(selfListener);
            } catch (InterpreterException e) {
                throw new IllegalStateException("Model update listener used on engine without base index", e);
            }
        }

        ChangeLevel changeLevel = listener.getLevel();
        listenerMap.computeIfAbsent(changeLevel, k -> CollectionsFactory.createSet()).add(listener);
        // increase or keep max level of listeners
        ChangeLevel oldMaxLevel = maxLevel;
        maxLevel = maxLevel.changeOccured(changeLevel);
        if(!maxLevel.equals(oldMaxLevel) && ChangeLevel.MATCHSET.compareTo(oldMaxLevel) > 0 && ChangeLevel.MATCHSET.compareTo(maxLevel) <= 0) {
            // add matchUpdateListener to all matchers
            for (InterpreterMatcher<?> matcher : this.queryEngine.getCurrentMatchers()) {
                this.queryEngine.addMatchUpdateListener(matcher, matchSetListener, false);
            }
        }
    }

    @Override
    protected void listenerRemoved(InterpreterModelUpdateListener listener) {
        ChangeLevel changeLevel = listener.getLevel();
        Collection<InterpreterModelUpdateListener> old = listenerMap.getOrDefault(changeLevel, Collections.emptySet());
        boolean removed = old.remove(listener);
        if(removed) {
            if (old.isEmpty()) listenerMap.remove(changeLevel);
        } else {
            handleUnsuccesfulRemove(listener);
        }

        updateMaxLevel();

        if(listenerMap.isEmpty()) {
            this.queryEngine.removeLifecycleListener(selfListener);
            removeBaseIndexChangeListener();
        }
    }

    private void removeBaseIndexChangeListener() {
        try {
            this.queryEngine.getBaseIndex().removeBaseIndexChangeListener(indexListener);
        } catch (InterpreterException e) {
            throw new IllegalStateException("Model update listener used on engine without base index", e);
        }
    }

    private void updateMaxLevel() {
        if(!listenerMap.containsKey(maxLevel)) {
            ChangeLevel newMaxLevel = ChangeLevel.NO_CHANGE;
            for (ChangeLevel level : new HashSet<>(listenerMap.keySet())) {
                newMaxLevel = newMaxLevel.changeOccured(level);
            }
            maxLevel = newMaxLevel;
        }
        if(maxLevel.compareTo(ChangeLevel.MATCHSET) < 0) {
            // remove listener from matchers
            for (InterpreterMatcher<?> matcher : this.queryEngine.getCurrentMatchers()) {
                this.queryEngine.removeMatchUpdateListener(matcher, matchSetListener);
            }
        }
    }

    private void handleUnsuccesfulRemove(InterpreterModelUpdateListener listener) {
        for (Entry<ChangeLevel, Collection<InterpreterModelUpdateListener>> entry : listenerMap.entrySet()) {
            Collection<InterpreterModelUpdateListener> existingListeners = entry.getValue();
            // if the listener is contained in some other bucket, remove it from there
            if(existingListeners.remove(listener)) {
                logger.error("Listener "+listener+" change level changed since initialization!");
                if (existingListeners.isEmpty()) listenerMap.remove(entry.getKey());
                return; // listener is contained only once
            }
        }
        logger.error("Listener "+listener+" already removed from map (e.g. engine was already disposed)!");
    }

    private void notifyListeners() {

        // any change that occurs after this point should be regarded as a new event
        // FIXME what should happen when a listener creates new notifications?
        // -> other listeners will get events in different order
        ChangeLevel tempLevel = currentChange;
        currentChange = ChangeLevel.NO_CHANGE;

        if(!listenerMap.isEmpty()) {
            for (ChangeLevel level : new HashSet<>(listenerMap.keySet())) {
                if(tempLevel.compareTo(level) >= 0) {
                    for (InterpreterModelUpdateListener listener : new ArrayList<>(listenerMap.get(level))) {
                        try {
                            listener.notifyChanged(tempLevel);
                        } catch (Exception ex) {
                            logger.error(
                                    "Refinery Interpreter encountered an error in delivering model update " +
											"notification to listener " + listener + ".", ex);
                        }
                    }
                }
            }
        } else {
            throw new IllegalStateException("Notify listeners must not be called without listeners! Maybe an update callback was not removed correctly.");
        }

    }

    // model update "providers":
    // - model: IQBase callback even if not dirty
    // - index: IQBase dirty callback
    private final InterpreterBaseIndexChangeListener indexListener = new InterpreterBaseIndexChangeListener() {

        @Override
        public boolean onlyOnIndexChange() {
            return false;
        }

        @Override
        public void notifyChanged(boolean indexChanged) {
            if(indexChanged) {
                currentChange = currentChange.changeOccured(ChangeLevel.INDEX);
            } else {
                currentChange = currentChange.changeOccured(ChangeLevel.MODEL);
            }
            notifyListeners();
        }

    };
    // - matchset: add the same listener to each matcher and use a dirty flag. needs IQBase callback as well
    private final IMatchUpdateListener<IPatternMatch> matchSetListener = new IMatchUpdateListener<IPatternMatch>() {

        @Override
        public void notifyDisappearance(IPatternMatch match) {
            currentChange = currentChange.changeOccured(ChangeLevel.MATCHSET);
        }

        @Override
        public void notifyAppearance(IPatternMatch match) {
            currentChange = currentChange.changeOccured(ChangeLevel.MATCHSET);
        }
    };

    private final InterpreterEngineLifecycleListener selfListener = new InterpreterEngineLifecycleListener() {

        @Override
        public void matcherInstantiated(InterpreterMatcher<? extends IPatternMatch> matcher) {
            if (maxLevel.compareTo(ChangeLevel.MATCHSET) >= 0) {
                ModelUpdateProvider.this.queryEngine.addMatchUpdateListener(matcher, matchSetListener, false);
            }
        }

        @Override
        public void engineWiped() {}

        @Override
        public void engineDisposed() {
            removeBaseIndexChangeListener();
            listenerMap.clear();
            maxLevel = ChangeLevel.NO_CHANGE;
        }

        @Override
        public void engineBecameTainted(String description, Throwable t) {}
    };
}
