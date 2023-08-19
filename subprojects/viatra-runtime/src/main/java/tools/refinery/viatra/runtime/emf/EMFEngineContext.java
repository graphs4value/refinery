/*******************************************************************************
 * Copyright (c) 2010-2014, Bergmann Gabor, Denes Harmath, Istvan Rath and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.viatra.runtime.emf;

import org.apache.log4j.Logger;
import org.eclipse.emf.common.notify.Notifier;
import tools.refinery.viatra.runtime.api.ViatraQueryEngine;
import tools.refinery.viatra.runtime.api.scope.IBaseIndex;
import tools.refinery.viatra.runtime.api.scope.IEngineContext;
import tools.refinery.viatra.runtime.api.scope.IIndexingErrorListener;
import tools.refinery.viatra.runtime.base.api.ViatraBaseFactory;
import tools.refinery.viatra.runtime.base.api.NavigationHelper;
import tools.refinery.viatra.runtime.matchers.ViatraQueryRuntimeException;
import tools.refinery.viatra.runtime.matchers.context.IQueryRuntimeContext;

/**
 * Implements an engine context on EMF models.
 * @author Bergmann Gabor
 *
 */
class EMFEngineContext implements IEngineContext {

    private final EMFScope emfScope;
    ViatraQueryEngine engine;
    Logger logger;
    NavigationHelper navHelper;
    IBaseIndex baseIndex;
    IIndexingErrorListener taintListener;
    private EMFQueryRuntimeContext runtimeContext;
    
    public EMFEngineContext(EMFScope emfScope, ViatraQueryEngine engine, IIndexingErrorListener taintListener, Logger logger) {
        this.emfScope = emfScope;
        this.engine = engine;
        this.logger = logger;
        this.taintListener = taintListener;
    }
    
    /**
     * @throws ViatraQueryRuntimeException thrown if the navigation helper cannot be initialized
     */
    public NavigationHelper getNavHelper() {
        return getNavHelper(true);
    }
    
    private NavigationHelper getNavHelper(boolean ensureInitialized) {
        if (navHelper == null) {
            // sync to avoid crazy compiler reordering which would matter if derived features use VIATRA and call this
            // reentrantly
            synchronized (this) {
                navHelper = ViatraBaseFactory.getInstance().createNavigationHelper(null, this.emfScope.getOptions(),
                        logger);
                getBaseIndex().addIndexingErrorListener(taintListener);
            }

            if (ensureInitialized) {
                ensureIndexLoaded();
            }

        }
        return navHelper;
    }

    private void ensureIndexLoaded() {
        for (Notifier scopeRoot : this.emfScope.getScopeRoots()) {
            navHelper.addRoot(scopeRoot);
        }
    }

    @Override
    public IQueryRuntimeContext getQueryRuntimeContext() {
        NavigationHelper nh = getNavHelper(false);
        if (runtimeContext == null) {
            runtimeContext = 
                    emfScope.getOptions().isDynamicEMFMode() ?
                     new DynamicEMFQueryRuntimeContext(nh, logger, emfScope) :
                     new EMFQueryRuntimeContext(nh, logger, emfScope);
                     
             ensureIndexLoaded();
        }
        
        return runtimeContext;
    }
    
    @Override
    public void dispose() {
        if (runtimeContext != null) runtimeContext.dispose();
        if (navHelper != null) navHelper.dispose();
        
        this.baseIndex = null;
        this.engine = null;
        this.logger = null;
        this.navHelper = null;
    }
    
    
    @Override
    public IBaseIndex getBaseIndex() {
        if (baseIndex == null) {
            final NavigationHelper navigationHelper = getNavHelper();
            baseIndex = new EMFBaseIndexWrapper(navigationHelper);
        }
        return baseIndex;
    }
}