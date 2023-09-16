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
import tools.refinery.interpreter.api.AdvancedInterpreterEngine;
import tools.refinery.interpreter.api.IPatternMatch;
import tools.refinery.interpreter.api.InterpreterEngineLifecycleListener;
import tools.refinery.interpreter.api.InterpreterMatcher;

import java.util.ArrayList;

public final class LifecycleProvider extends ListenerContainer<InterpreterEngineLifecycleListener> implements InterpreterEngineLifecycleListener {

        private final Logger logger;

        /**
         * @param queryEngine
         */
        public LifecycleProvider(AdvancedInterpreterEngine queryEngine, Logger logger) {
            this.logger = logger;
        }

        @Override
        protected void listenerAdded(InterpreterEngineLifecycleListener listener) {
            logger.debug("Lifecycle listener " + listener + " added to engine.");
        }

        @Override
        protected void listenerRemoved(InterpreterEngineLifecycleListener listener) {
            logger.debug("Lifecycle listener " + listener + " removed from engine.");
        }

//        public void propagateEventToListeners(Predicate<ViatraQueryEngineLifecycleListener> function) {
//            if (!listeners.isEmpty()) {
//                for (ViatraQueryEngineLifecycleListener listener : new ArrayList<ViatraQueryEngineLifecycleListener>(listeners)) {
//                    try {
//                        function.apply(listener);
//                    } catch (Exception ex) {
//                        logger.error(
//                                "Refinery Interpreter encountered an error in delivering notification to listener "
//                                        + listener + ".", ex);
//                    }
//                }
//            }
//        }

        @Override
        public void matcherInstantiated(final InterpreterMatcher<? extends IPatternMatch> matcher) {
            if (!listeners.isEmpty()) {
                for (InterpreterEngineLifecycleListener listener : new ArrayList<InterpreterEngineLifecycleListener>(listeners)) {
                    try {
                        listener.matcherInstantiated(matcher);
                    } catch (Exception ex) {
                        logger.error(
                                "Refinery Interpreter encountered an error in delivering matcher initialization " +
										"notification to listener " + listener + ".", ex);
                    }
                }
            }
//            propagateEventToListeners(new Predicate<ViatraQueryEngineLifecycleListener>() {
//               public boolean apply(ViatraQueryEngineLifecycleListener listener) {
//                   listener.matcherInstantiated(matcher);
//                   return true;
//               }
//            });
        }

        @Override
        public void engineBecameTainted(String description, Throwable t) {
            if (!listeners.isEmpty()) {
                for (InterpreterEngineLifecycleListener listener : new ArrayList<InterpreterEngineLifecycleListener>(listeners)) {
                    try {
                        listener.engineBecameTainted(description, t);
                    } catch (Exception ex) {
                        logger.error(
                                "Refinery Interpreter encountered an error in delivering engine tainted notification " +
										"to listener " + listener + ".", ex);
                    }
                }
            }
//            propagateEventToListeners(new Predicate<ViatraQueryEngineLifecycleListener>() {
//                public boolean apply(ViatraQueryEngineLifecycleListener listener) {
//                    listener.engineBecameTainted();
//                    return true;
//                }
//             });
        }

        @Override
        public void engineWiped() {
            if (!listeners.isEmpty()) {
                for (InterpreterEngineLifecycleListener listener : new ArrayList<InterpreterEngineLifecycleListener>(listeners)) {
                    try {
                        listener.engineWiped();
                    } catch (Exception ex) {
                        logger.error(
                                "Refinery Interpreter encountered an error in delivering engine wiped notification to" +
										" listener " + listener + ".", ex);
                    }
                }
            }
//            propagateEventToListeners(new Predicate<ViatraQueryEngineLifecycleListener>() {
//                public boolean apply(ViatraQueryEngineLifecycleListener listener) {
//                    listener.engineWiped();
//                    return true;
//                }
//             });
        }

        @Override
        public void engineDisposed() {
            if (!listeners.isEmpty()) {
                for (InterpreterEngineLifecycleListener listener : new ArrayList<InterpreterEngineLifecycleListener>(listeners)) {
                    try {
                        listener.engineDisposed();
                    } catch (Exception ex) {
                        logger.error(
                                "Refinery Interpreter encountered an error in delivering engine disposed notification" +
										" to listener " + listener + ".", ex);
                    }
                }
            }
//            propagateEventToListeners(new Predicate<ViatraQueryEngineLifecycleListener>() {
//                public boolean apply(ViatraQueryEngineLifecycleListener listener) {
//                    listener.engineDisposed();
//                    return true;
//                }
//             });
        }

    }
