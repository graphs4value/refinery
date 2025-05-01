/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.query.interpreter.internal.context;

import tools.refinery.interpreter.api.scope.IBaseIndex;
import tools.refinery.interpreter.api.scope.IIndexingErrorListener;
import tools.refinery.interpreter.api.scope.IInstanceObserver;
import tools.refinery.interpreter.api.scope.InterpreterBaseIndexChangeListener;

import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.Callable;

/**
 * Copied from
 * <a href="https://github.com/eclipse-viatra/org.eclipse.viatra/blob/1c149b3b67d8e5d9e96dfccd4cfd77f4a3d0a2ee/query/plugins/org.eclipse.viatra.query.runtime/src/org/eclipse/viatra/query/runtime/tabular/TabularEngineContext.java">TabularEngineContext</a>
 */
public class DummyBaseIndexer implements IBaseIndex {
	DummyBaseIndexer() {
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
	public void addBaseIndexChangeListener(InterpreterBaseIndexChangeListener listener) {
		// no notification support
	}

	@Override
	public void removeBaseIndexChangeListener(InterpreterBaseIndexChangeListener listener) {
		// no notification support
	}

	@Override
	public void resampleDerivedFeatures() {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean addIndexingErrorListener(IIndexingErrorListener listener) {
		return false;
	}

	@Override
	public boolean removeIndexingErrorListener(IIndexingErrorListener listener) {
		return false;
	}

	@Override
	public boolean addInstanceObserver(IInstanceObserver observer, Object observedObject) {
		return false;
	}

	@Override
	public boolean removeInstanceObserver(IInstanceObserver observer, Object observedObject) {
		return false;
	}
}
