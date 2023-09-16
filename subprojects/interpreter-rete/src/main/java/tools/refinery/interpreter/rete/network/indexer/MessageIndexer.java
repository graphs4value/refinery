/*******************************************************************************
 * Copyright (c) 2010-2018, Tamas Szabo, Istvan Rath and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.interpreter.rete.network.indexer;

import tools.refinery.interpreter.rete.network.mailbox.Mailbox;
import tools.refinery.interpreter.matchers.tuple.Tuple;
import tools.refinery.interpreter.matchers.util.Clearable;

/**
 * A message indexer is used by {@link Mailbox}es to index their contents.
 *
 * @author Tamas Szabo
 * @since 2.0
 */
public interface MessageIndexer extends Clearable {

	public void insert(final Tuple update);

	public void delete(final Tuple update);

	public void update(final Tuple update, final int delta);

	public boolean isEmpty();

	public int getCount(final Tuple update);

}
