/*******************************************************************************
 * Copyright (c) 2010-2018, Tamas Szabo, Istvan Rath and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.interpreter.rete.network.indexer;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import tools.refinery.interpreter.matchers.tuple.Tuple;
import tools.refinery.interpreter.matchers.tuple.TupleMask;
import tools.refinery.interpreter.matchers.util.CollectionsFactory;

/**
 * @author Tamas Szabo
 * @since 2.0
 */
public class GroupBasedMessageIndexer implements MessageIndexer {

	protected final Map<Tuple, DefaultMessageIndexer> indexer;
	protected final TupleMask groupMask;

	public GroupBasedMessageIndexer(final TupleMask groupMask) {
		this.indexer = CollectionsFactory.createMap();
		this.groupMask = groupMask;
	}

	public Map<Tuple, Integer> getTuplesByGroup(final Tuple group) {
		final DefaultMessageIndexer values = this.indexer.get(group);
		if (values == null) {
			return Collections.emptyMap();
		} else {
			return Collections.unmodifiableMap(values.getTuples());
		}
	}

	@Override
	public int getCount(final Tuple update) {
		final Tuple group = this.groupMask.transform(update);
		final Integer count = getTuplesByGroup(group).get(update);
		if (count == null) {
			return 0;
		} else {
			return count;
		}
	}

	public Set<Tuple> getGroups() {
		return Collections.unmodifiableSet(this.indexer.keySet());
	}

	@Override
	public void insert(final Tuple update) {
		update(update, 1);
	}

	@Override
	public void delete(final Tuple update) {
		update(update, -1);
	}

	@Override
	public void update(final Tuple update, final int delta) {
		final Tuple group = this.groupMask.transform(update);
		DefaultMessageIndexer valueIndexer = this.indexer.get(group);

		if (valueIndexer == null) {
			valueIndexer = new DefaultMessageIndexer();
			this.indexer.put(group, valueIndexer);
		}

		valueIndexer.update(update, delta);

		// it may happen that the indexer becomes empty as a result of the update
		if (valueIndexer.isEmpty()) {
			this.indexer.remove(group);
		}
	}

	@Override
	public boolean isEmpty() {
		return this.indexer.isEmpty();
	}

	@Override
	public void clear() {
		this.indexer.clear();
	}

}
