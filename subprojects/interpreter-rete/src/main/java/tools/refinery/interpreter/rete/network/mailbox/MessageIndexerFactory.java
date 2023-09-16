/*******************************************************************************
 * Copyright (c) 2010-2018, Tamas Szabo, Istvan Rath and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.interpreter.rete.network.mailbox;

import tools.refinery.interpreter.rete.network.indexer.MessageIndexer;

/**
 * A factory used to create message indexers for {@link Mailbox}es.
 *
 * @author Tamas Szabo
 * @since 2.0
 */
public interface MessageIndexerFactory<I extends MessageIndexer> {

    public I create();

}
