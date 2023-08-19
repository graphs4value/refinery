/*******************************************************************************
 * Copyright (c) 2010-2018, Gabor Bergmann, IncQuery Labs Ltd.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.viatra.runtime.matchers.scopes;

import tools.refinery.viatra.runtime.matchers.context.IInputKey;
import tools.refinery.viatra.runtime.matchers.scopes.tables.ITableContext;
import tools.refinery.viatra.runtime.matchers.scopes.tables.SimpleBinaryTable;
import tools.refinery.viatra.runtime.matchers.scopes.tables.SimpleUnaryTable;

/**
 * Basic storage backend implementation based on local collections.
 * 
 * <p><strong>EXPERIMENTAL</strong>. This class or interface has been added as
 * part of a work in progress. There is no guarantee that this API will
 * work or that it will remain the same.
 * 
 * @author Gabor Bergmann
 * @since 2.1
 */
public class SimpleLocalStorageBackend implements IStorageBackend {

	@Override
	public void startTransaction() {
		// NOP
	}

	@Override
	public void finishTransaction() {
		// NOP
	}

	@Override
	public SimpleUnaryTable<Object> createUnaryTable(IInputKey key, ITableContext tableContext, boolean unique) {
		return new SimpleUnaryTable<>(key, tableContext, unique);
	}

	@Override
	public SimpleBinaryTable<Object, Object> createBinaryTable(IInputKey key, ITableContext tableContext,
															   boolean unique) {
		return new SimpleBinaryTable<>(key, tableContext, unique);
	}



}
