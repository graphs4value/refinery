/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.dse.modification;

import tools.refinery.store.adapter.ModelAdapter;
import tools.refinery.store.dse.modification.internal.ModificationBuilderImpl;
import tools.refinery.store.tuple.Tuple;
import tools.refinery.store.tuple.Tuple1;

public interface ModificationAdapter extends ModelAdapter {

	int getModelSize();

	Tuple1 createObject();

	boolean deleteObject(Tuple tuple, DanglingEdges danglingEdges);

	static ModificationBuilder builder() {
		return new ModificationBuilderImpl();
	}
}
