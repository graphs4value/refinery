/*******************************************************************************
 * Copyright (c) 2010-2018, Gabor Bergmann, IncQuery Labs Ltd.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.viatra.runtime.tabular;

import tools.refinery.viatra.runtime.api.ViatraQueryEngine;
import tools.refinery.viatra.runtime.api.scope.IEngineContext;
import tools.refinery.viatra.runtime.api.scope.IIndexingErrorListener;
import tools.refinery.viatra.runtime.api.scope.QueryScope;
import tools.refinery.viatra.runtime.matchers.context.IInputKey;
import tools.refinery.viatra.runtime.matchers.scopes.IStorageBackend;
import tools.refinery.viatra.runtime.matchers.scopes.TabularRuntimeContext;
import tools.refinery.viatra.runtime.matchers.scopes.tables.IIndexTable;
import tools.refinery.viatra.runtime.matchers.scopes.tables.ITableWriterBinary;
import tools.refinery.viatra.runtime.matchers.scopes.tables.ITableWriterUnary;

/**
 * Simple tabular index host. 
 * 
 * Unlike traditional Viatra instances initialized on a model, 
 *  this index host can be initialized and then queried, 
 *  while its queriable "contents" (base relations) can be separately written using table writer API. 
 *  
 * <p> Deriving classes are responsible for setting up the tables of this index and providing the writer API to clients.
 *  For the former, use {@link #newUnaryInputTable(IInputKey, boolean)}, 
 *  {@link #newBinaryInputTable(IInputKey, boolean)} to create input tables, 
 *  or {@link #registerNewTable(IIndexTable)} to register manually created derived tables. 
 *  Instantiate such tables with {@link #runtimeContext} as their table context.
 *  
 * 
 * <p>
 * <strong>EXPERIMENTAL</strong>. This class or interface has been added as
 * part of a work in progress. There is no guarantee that this API will
 * work or that it will remain the same.
 *
 * @see EcoreIndexHost EcoreIndexHost for EMF-specific example usage.
 * 
 * @author Gabor Bergmann
 * @since 2.1
 */
public abstract class TabularIndexHost {
    
    private final IStorageBackend storage;
    protected final TabularRuntimeContext runtimeContext;
    protected final TabularIndexScope scope = new TabularIndexScope();

    public TabularIndexHost(IStorageBackend storage, TabularRuntimeContext runtimeContext) {
        this.storage = storage;
        this.runtimeContext = runtimeContext;
    }


    public TabularRuntimeContext getRuntimeContext() {
        return runtimeContext;
    }

    public TabularIndexScope getScope() {
        return scope;
    }
    
    /**
     * @return true if this index host aims to serve queries that have a scope of the given type
     */
    protected abstract boolean isQueryScopeEmulated(Class<? extends QueryScope> queryScopeClass);

    
    
    /**
     * Marks the beginning of an update transaction.
     * In transaction mode, index table updates may be temporarily delayed for better performance. 
     * Stateful query backends will not update their results during the index update transaction. (TODO actually achieve this)
     */
    public void startUpdateTransaction() {
        storage.startTransaction();
    }
    /**
     * Marks the end of an update transaction.
     * Any updates to index tables that were delayed during the transaction must now be flushed. 
     * Afterwards, stateful query backends will update their results. (TODO actually achieve this)
     */
    public void finishUpdateTransaction() {
        storage.finishTransaction();
    }
    
    /**
     * To be called by deriving class. Creates and registers a new unary input table.
     */
    protected ITableWriterUnary.Table<Object> newUnaryInputTable(IInputKey key, boolean unique) {
        return registerNewTable(storage.createUnaryTable(key, runtimeContext, unique));
    }
    /**
     * To be called by deriving class. Creates and registers a new binary input table.
     */
    protected ITableWriterBinary.Table<Object,Object> newBinaryInputTable(IInputKey key, boolean unique) {
        return registerNewTable(storage.createBinaryTable(key, runtimeContext, unique));
    }
    /**
     * To be called by deriving class. Registers the given freshly created table and also returns it for convenience.
     */
    protected <Table extends IIndexTable> Table registerNewTable(Table newTable) {
        runtimeContext.registerIndexTable(newTable);
        return newTable;
    }


    /**
     * A scope describing queries evaluated against tzhis index host.
     * @author Gabor Bergmann
     *
     */
    public class TabularIndexScope extends QueryScope {
        
        public TabularIndexHost getIndexHost() {
            return TabularIndexHost.this;
        }
        
        @Override
        public int hashCode() {
            return getIndexHost().hashCode();
        }
        
        @Override
        public boolean equals(Object obj) {
            if (obj instanceof TabularIndexScope)
                return getIndexHost().equals(((TabularIndexScope) obj).getIndexHost());
            return false;
        }
        
        @Override
        public boolean isCompatibleWithQueryScope(Class<? extends QueryScope> queryScopeClass) {
            return isQueryScopeEmulated(queryScopeClass) || super.isCompatibleWithQueryScope(queryScopeClass);
        }
        
        @Override
        protected IEngineContext createEngineContext(ViatraQueryEngine engine, IIndexingErrorListener errorListener, org.apache.log4j.Logger logger) {
            return new TabularEngineContext(getIndexHost(), engine, errorListener, logger);
        }

    }
}
