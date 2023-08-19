/*******************************************************************************
 * Copyright (c) 2010-2018, Gabor Bergmann, IncQuery Labs Ltd.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package tools.refinery.viatra.runtime.matchers.scopes;

import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.Callable;

import tools.refinery.viatra.runtime.matchers.context.IInputKey;
import tools.refinery.viatra.runtime.matchers.context.IQueryMetaContext;
import tools.refinery.viatra.runtime.matchers.context.IndexingService;
import tools.refinery.viatra.runtime.matchers.context.common.JavaTransitiveInstancesKey;
import tools.refinery.viatra.runtime.matchers.scopes.tables.IIndexTable;
import tools.refinery.viatra.runtime.matchers.tuple.ITuple;
import tools.refinery.viatra.runtime.matchers.tuple.Tuple;

/**
 * A simple demo implementation of the IQRC interface using tables.
 * 
 * <p>
 * Usage: first, instantiate {@link IIndexTable} tables with this as the 'tableContext' argument, and call
 * {@link #registerIndexTable(IIndexTable)} manually to register them. Afterwards, they will be visible to the query
 * backends.
 * <p>
 * <strong>EXPERIMENTAL</strong>. This class or interface has been added as
 * part of a work in progress. There is no guarantee that this API will
 * work or that it will remain the same.
 *
 * @author Gabor Bergmann
 * @since 2.0
 */
public class SimpleRuntimeContext extends TabularRuntimeContext {

    private IQueryMetaContext metaContext;

    public SimpleRuntimeContext(IQueryMetaContext metaContext) {
        this.metaContext = metaContext;
    }

    @Override
    public void logError(String message) {
        System.err.println(message);
    }

    @Override
    public IQueryMetaContext getMetaContext() {
        return metaContext;
    }

    @Override
    public <V> V coalesceTraversals(Callable<V> callable) throws InvocationTargetException {
        try {
            return callable.call();
        } catch (Exception e) {
            throw new InvocationTargetException(e);
        }
    }

    @Override
    public boolean isCoalescing() {
        return false;
    }

    @Override
    public boolean isIndexed(IInputKey key, IndexingService service) {
        return peekIndexTable(key) != null;
    }

    @Override
    public void ensureIndexed(IInputKey key, IndexingService service) {
        if (peekIndexTable(key) == null)
            throw new IllegalArgumentException(key.getPrettyPrintableName());
    }

    @Override
    public Object wrapElement(Object externalElement) {
        return externalElement;
    }

    @Override
    public Object unwrapElement(Object internalElement) {
        return internalElement;
    }

    @Override
    public Tuple wrapTuple(Tuple externalElements) {
        return externalElements;
    }

    @Override
    public Tuple unwrapTuple(Tuple internalElements) {
        return internalElements;
    }

    @Override
    public void ensureWildcardIndexing(IndexingService service) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void executeAfterTraversal(Runnable runnable) throws InvocationTargetException {
        runnable.run();
    }

    @Override
    protected boolean isContainedInStatelessKey(IInputKey key, ITuple seed) {
        if (key instanceof JavaTransitiveInstancesKey) {
            Class<?> instanceClass = forceGetWrapperInstanceClass((JavaTransitiveInstancesKey) key);
            return instanceClass != null && instanceClass.isInstance(seed.get(0));
        } else
            throw new IllegalArgumentException(key.getPrettyPrintableName());
    }

    private Class<?> forceGetWrapperInstanceClass(JavaTransitiveInstancesKey key) {
        Class<?> instanceClass;
        try {
            instanceClass = key.forceGetWrapperInstanceClass();
        } catch (ClassNotFoundException e) {
            logError(
                    "Could not load instance class for type constraint " + key.getWrappedKey() + ": " + e.getMessage());
            instanceClass = null;
        }
        return instanceClass;
    }

}
