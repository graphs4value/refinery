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
import tools.refinery.viatra.runtime.matchers.scopes.tables.ITableWriterBinary;
import tools.refinery.viatra.runtime.matchers.scopes.tables.ITableWriterUnary;

/**
 * An abstract storage backend that instantiates tables and coordinates transactions.
 * 
 * <p><strong>EXPERIMENTAL</strong>. This class or interface has been added as
 * part of a work in progress. There is no guarantee that this API will
 * work or that it will remain the same.
 * 
 * @author Gabor Bergmann
 * 
 * @since 2.1
 */
public interface IStorageBackend {


	/**
	 * Marks the beginning of a transaction.
	 * In transaction mode, table updates may be temporarily delayed ({@link tools.refinery.viatra.runtime.matchers.scopes.tables.IIndexTable} methods may return stale answers) for better performance.
	 */
	void startTransaction();
	/**
	 * Marks the end of a transaction.
	 * Any updates delayed during the transaction must now be flushed. 
	 */
	void finishTransaction();

	/**
	 * Creates an index table for a simple value set.
     * @param unique client promises to only insert a given tuple with multiplicity one
	 */
	ITableWriterUnary.Table<Object> createUnaryTable(IInputKey key, ITableContext tableContext, boolean unique);
	/**
	 * Creates an index table for a simple source-target bidirectional mapping.
     * @param unique client promises to only insert a given tuple with multiplicity one
	 */
	ITableWriterBinary.Table<Object,Object> createBinaryTable(IInputKey key, ITableContext tableContext, boolean unique);
	

}
