/*******************************************************************************
 * Copyright (c) 2004-2008 Gabor Bergmann and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package tools.refinery.viatra.runtime.api;

import static tools.refinery.viatra.runtime.matchers.util.Preconditions.checkArgument;

import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import tools.refinery.viatra.runtime.api.scope.QueryScope;
import tools.refinery.viatra.runtime.emf.EMFScope;
import tools.refinery.viatra.runtime.internal.apiimpl.ViatraQueryEngineImpl;
import tools.refinery.viatra.runtime.util.ViatraQueryLoggingUtil;

/**
 * Global registry of active VIATRA Query Engines.
 * 
 * <p>
 * Manages an {@link ViatraQueryEngine} for each model (more precisely scope), that is created on demand. Managed engines are shared between
 * clients querying the same model.
 * 
 * <p>
 * It is also possible to create private, unmanaged engines that are not shared between clients.
 * 
 * <p>
 * Only weak references are retained on the managed engines. So if there are no other references to the matchers or the
 * engine, they can eventually be GC'ed, and they won't block the model from being GC'ed either.
 * 
 * 
 * @author Bergmann Gabor
 * 
 */
public class ViatraQueryEngineManager {
    private static ViatraQueryEngineManager instance = new ViatraQueryEngineManager();
    

    /**
     * @return the singleton instance
     */
    public static ViatraQueryEngineManager getInstance() {
        return instance;
    }

    /**
     * The engine manager keeps track of the managed engines via weak references only, so it will not keep unused
     * managed engines from being garbage collected. Still, as long as the user keeps the model in memory, the
     * associated managed engine will not be garbage collected, as it is expected to be strongly reachable from the
     * model via the scope-specific base index or change notification listeners (see
     * {@link BaseIndexListener#iqEngine}).
     */
    Map<QueryScope, WeakReference<ViatraQueryEngineImpl>> engines;

    ViatraQueryEngineManager() {
        super();
        engines = new WeakHashMap<QueryScope, WeakReference<ViatraQueryEngineImpl>>();
        initializationListeners = new HashSet<ViatraQueryEngineInitializationListener>();
    }

    /**
     * Creates a managed VIATRA Query Engine at a given scope (e.g. an EMF Resource or ResourceSet, as in {@link EMFScope}) 
     * or retrieves an already existing one. Repeated invocations for a single model root will return the same engine. 
     * Consequently, the engine will be reused between different clients querying the same model, providing performance benefits.
     * 
     * <p>
     * The match set of any patterns will be incrementally refreshed upon updates from this scope.
     * 
     * @param scope 
     * 		the scope of query evaluation; the definition of the set of model elements that this engine is operates on. 
     * 		Provide e.g. a {@link EMFScope} for evaluating queries on an EMF model.
     * @return a new or previously existing engine
     */
    public ViatraQueryEngine getQueryEngine(QueryScope scope) {
        return getQueryEngine(scope, ViatraQueryEngineOptions.getDefault());
    }
    
    /**
     * Creates a managed VIATRA Query Engine at a given scope (e.g. an EMF Resource or ResourceSet, as in {@link EMFScope}) 
     * or retrieves an already existing one. Repeated invocations for a single model root will return the same engine. 
     * Consequently, the engine will be reused between different clients querying the same model, providing performance benefits.
     * 
     * <p>
     * The match set of any patterns will be incrementally refreshed upon updates from this scope.
     * 
     * @param scope 
     *      the scope of query evaluation; the definition of the set of model elements that this engine is operates on. 
     *      Provide e.g. a {@link EMFScope} for evaluating queries on an EMF model.
     * @return a new or previously existing engine
     * @since 1.4
     */
    public ViatraQueryEngine getQueryEngine(QueryScope scope, ViatraQueryEngineOptions options) {
        ViatraQueryEngineImpl engine = getEngineInternal(scope);
        if (engine == null) {
            engine = new ViatraQueryEngineImpl(this, scope, options);
            engines.put(scope, new WeakReference<ViatraQueryEngineImpl>(engine));
            notifyInitializationListeners(engine);
        }
        return engine;
    }

    /**
     * Retrieves an already existing managed VIATRA Query Engine.
     * 
     * @param scope 
     * 		the scope of query evaluation; the definition of the set of model elements that this engine is operates on. 
     * 		Provide e.g. a {@link EMFScope} for evaluating queries on an EMF model.
     * @return a previously existing engine, or null if no engine is active for the given EMF model root
     */
    public ViatraQueryEngine getQueryEngineIfExists(QueryScope scope) {
        return getEngineInternal(scope);
    }

    /**
     * Collects all {@link ViatraQueryEngine} instances that still exist.
     * 
     * @return set of engines if there is any, otherwise EMTPY_SET
     */
    public Set<ViatraQueryEngine> getExistingQueryEngines(){
        Set<ViatraQueryEngine> existingEngines = null;
        for (WeakReference<ViatraQueryEngineImpl> engineRef : engines.values()) {
            AdvancedViatraQueryEngine engine = engineRef == null ? null : engineRef.get();
            if(existingEngines == null) {
                existingEngines = new HashSet<>();
            }
            existingEngines.add(engine);
        }
        if(existingEngines == null) {
            existingEngines = Collections.emptySet();
        }
        return existingEngines;
    }
        
    private final Set<ViatraQueryEngineInitializationListener> initializationListeners;
    
    /**
     * Registers a listener for new engine initialization.
     * 
     * <p/> For removal, use {@link #removeQueryEngineInitializationListener}
     * 
     * @param listener the listener to register
     */
    public void addQueryEngineInitializationListener(ViatraQueryEngineInitializationListener listener) {
        checkArgument(listener != null, "Cannot add null listener!");
        initializationListeners.add(listener);
    }

    /**
     * Removes a registered listener added with {@link #addQueryEngineInitializationListener}
     * 
     * @param listener
     */
    public void removeQueryEngineInitializationListener(ViatraQueryEngineInitializationListener listener) {
        checkArgument(listener != null, "Cannot remove null listener!");
        initializationListeners.remove(listener);
    }

    /**
     * Notifies listeners that a new engine has been initialized.
     * 
     * @param engine the initialized engine
     */
    protected void notifyInitializationListeners(AdvancedViatraQueryEngine engine) {
        try {
            if (!initializationListeners.isEmpty()) {
                for (ViatraQueryEngineInitializationListener listener : new HashSet<>(initializationListeners)) {
                    listener.engineInitialized(engine);
                }
            }
        } catch (Exception ex) {
            ViatraQueryLoggingUtil.getLogger(getClass()).fatal(
                    "VIATRA Query Engine Manager encountered an error in delivering notifications"
                            + " about engine initialization. ", ex);
        }
    }

    /**
     * @param emfRoot
     * @return
     */
    private ViatraQueryEngineImpl getEngineInternal(QueryScope scope) {
        final WeakReference<ViatraQueryEngineImpl> engineRef = engines.get(scope);
        ViatraQueryEngineImpl engine = engineRef == null ? null : engineRef.get();
        return engine;
    }

}
